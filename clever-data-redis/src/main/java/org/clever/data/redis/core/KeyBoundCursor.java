package org.clever.data.redis.core;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 17:37 <br/>
 */
public abstract class KeyBoundCursor<T> extends ScanCursor<T> {
    private final byte[] key;

    /**
     * 装箱新的 {@link ScanCursor}
     *
     * @param options 如果为空，则默认为 {@link ScanOptions#NONE}
     */
    public KeyBoundCursor(byte[] key, long cursorId, ScanOptions options) {
        super(cursorId, options != null ? options : ScanOptions.NONE);
        this.key = key;
    }

    protected ScanIteration<T> doScan(long cursorId, ScanOptions options) {
        return doScan(this.key, cursorId, options);
    }

    protected abstract ScanIteration<T> doScan(byte[] key, long cursorId, ScanOptions options);

    public byte[] getKey() {
        return key;
    }
}
