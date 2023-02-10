package org.clever.data.redis.core;

import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.util.CollectionUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Redis客户端不可知的 {@link Cursor} 实现不断从Redis服务器加载其他结果，直到到达其起点 {@code zero}。 <br />
 * <strong>注意:</strong> 请注意， {@link ScanCursor} 必须在使用前初始化 {@link #open()}。
 * 扫描过程中的任何失败都将 {@link #close() close} 光标并释放任何相关资源，如连接。
 * Redis客户端不可知的 {@link Cursor} 实现不断从Redis服务器加载其他结果，直到到达其起点 {@code zero} 。<br />
 * <strong>注意:</strong> 请注意，{@link ScanCursor} 必须在使用前初始化 {@link #open()}。
 * 扫描过程中的任何失败都将 {@link #close() close} 光标并释放任何相关资源，如连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:38 <br/>
 */
public abstract class ScanCursor<T> implements Cursor<T> {
    private CursorState state;
    private long cursorId;
    private Iterator<T> delegate;
    private final ScanOptions scanOptions;
    private long position;

    /**
     * 用 {@code id=0} 和 {@link ScanOptions#NONE} 包装新的 {@link ScanCursor}
     */
    public ScanCursor() {
        this(ScanOptions.NONE);
    }

    /**
     * 用 {@code id=0} 包装新的 {@link ScanCursor}
     *
     * @param options 要应用的扫描选项
     */
    public ScanCursor(ScanOptions options) {
        this(0, options);
    }

    /**
     * 用 {@link ScanOptions#NONE} 包装新的  {@link ScanCursor}
     *
     * @param cursorId 光标id
     */
    public ScanCursor(long cursorId) {
        this(cursorId, ScanOptions.NONE);
    }

    /**
     * 装箱新的  {@link ScanCursor}
     *
     * @param cursorId 光标id
     * @param options  如果 {@code null}，则默认为 {@link ScanOptions#NONE}
     */
    public ScanCursor(long cursorId, ScanOptions options) {
        this.scanOptions = options != null ? options : ScanOptions.NONE;
        this.cursorId = cursorId;
        this.state = CursorState.READY;
        this.delegate = Collections.emptyIterator();
    }

    private void scan(long cursorId) {
        try {
            processScanResult(doScan(cursorId, this.scanOptions));
        } catch (RuntimeException e) {
            try {
                close();
            } catch (RuntimeException nested) {
                e.addSuppressed(nested);
            }
            throw e;
        }
    }

    /**
     * 使用本机客户端实现执行实际扫描命令。给定的 {@literal options} 永远不是 {@code null}
     */
    protected abstract ScanIteration<T> doScan(long cursorId, ScanOptions options);

    /**
     * 使用前初始化  {@link Cursor}
     */
    public final ScanCursor<T> open() {
        if (!isReady()) {
            throw new InvalidDataAccessApiUsageException("Cursor already " + state + ". Cannot (re)open it.");
        }
        state = CursorState.OPEN;
        doOpen(cursorId);
        return this;
    }

    /**
     * 调用 {@link #open()} 时的自定义挂钩
     */
    protected void doOpen(long cursorId) {
        scan(cursorId);
    }

    private void processScanResult(ScanIteration<T> result) {
        cursorId = result.getCursorId();
        if (isFinished(cursorId)) {
            state = CursorState.FINISHED;
        }
        if (!CollectionUtils.isEmpty(result.getItems())) {
            delegate = result.iterator();
        } else {
            resetDelegate();
        }
    }

    /**
     * 检查 {@code cursorId} 是否完成。
     *
     * @param cursorId 光标 id
     * @return {@literal true} 如果光标被认为已完成，则 {@literal false} 否则
     */
    protected boolean isFinished(long cursorId) {
        return cursorId == 0;
    }

    private void resetDelegate() {
        delegate = Collections.emptyIterator();
    }

    @Override
    public long getCursorId() {
        return cursorId;
    }

    @Override
    public boolean hasNext() {
        assertCursorIsOpen();
        while (!delegate.hasNext() && !CursorState.FINISHED.equals(state)) {
            scan(cursorId);
        }
        if (delegate.hasNext()) {
            return true;
        }
        return cursorId > 0;
    }

    private void assertCursorIsOpen() {
        if (isReady() || isClosed()) {
            throw new InvalidDataAccessApiUsageException("Cannot access closed cursor. Did you forget to call open()?");
        }
    }

    @Override
    public T next() {
        assertCursorIsOpen();
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements available for cursor " + cursorId + ".");
        }
        T next = moveNext(delegate);
        position++;
        return next;
    }

    /**
     * 从基础 {@link Iterable} 获取下一项
     */
    protected T moveNext(Iterator<T> source) {
        return source.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported");
    }

    @Override
    public final void close() {
        try {
            doClose();
        } finally {
            state = CursorState.CLOSED;
        }
    }

    /**
     * 用于在调用 {@link #close()} 时清理资源的自定义挂钩
     */
    protected void doClose() {
    }

    @Override
    public boolean isClosed() {
        return state == CursorState.CLOSED;
    }

    protected final boolean isReady() {
        return state == CursorState.READY;
    }

    protected final boolean isOpen() {
        return state == CursorState.OPEN;
    }

    @Override
    public long getPosition() {
        return position;
    }

    enum CursorState {
        READY, OPEN, FINISHED, CLOSED;
    }
}
