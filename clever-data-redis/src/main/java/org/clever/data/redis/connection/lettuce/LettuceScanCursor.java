package org.clever.data.redis.connection.lettuce;

import org.clever.data.redis.core.ScanCursor;
import org.clever.data.redis.core.ScanIteration;
import org.clever.data.redis.core.ScanOptions;

import java.util.Collection;

/**
 * 特定于生菜的 {@link ScanCursor} 扩展，用于维护跨 Redis 集群进行有状态扫描所需的游标状态。
 * <p>
 * 游标状态使用 Lettuce 的有状态 {@link io.lettuce.core.ScanCursor} 来跟踪扫描进度。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/30 22:03 <br/>
 */
abstract class LettuceScanCursor<T> extends ScanCursor<T> {
    private io.lettuce.core.ScanCursor state;

    /**
     * 创建一个新的 {@link LettuceScanCursor} 给定 {@link ScanOptions}
     *
     * @param options 不得为 {@literal null}
     */
    LettuceScanCursor(ScanOptions options) {
        super(options);
    }

    @Override
    protected ScanIteration<T> doScan(long cursorId, ScanOptions options) {
        if (state == null && cursorId == 0) {
            return scanAndProcessState(io.lettuce.core.ScanCursor.INITIAL, options);
        }
        if (state != null) {
            if (isMatchingCursor(cursorId)) {
                return scanAndProcessState(state, options);
            }
        }
        throw new IllegalArgumentException(String.format(
                "Current scan %s state and cursor %d do not match!",
                state != null ? state.getCursor() : "(none)", cursorId
        ));
    }

    @Override
    protected boolean isFinished(long cursorId) {
        return state != null && isMatchingCursor(cursorId) ? state.isFinished() : super.isFinished(cursorId);
    }

    private ScanIteration<T> scanAndProcessState(io.lettuce.core.ScanCursor scanCursor, ScanOptions options) {
        LettuceScanIteration<T> iteration = doScan(scanCursor, options);
        state = iteration.cursor;
        return iteration;
    }

    private boolean isMatchingCursor(long cursorId) {
        return state != null && state.getCursor().equals(Long.toString(cursorId));
    }

    /**
     * 执行给定 {@link io.lettuce.core.ScanCursor} 和 {@link ScanOptions} 的实际扫描操作
     *
     * @param cursor  不得为 {@literal null}
     * @param options 不得为 {@literal null}
     * @return 从不为 {@literal null}
     */
    protected abstract LettuceScanIteration<T> doScan(io.lettuce.core.ScanCursor cursor, ScanOptions options);

    /**
     * 对 {@link ScanIteration} 的特定于生菜的扩展，用于跟踪原始 {@link io.lettuce.core.ScanCursor} 对象
     */
    static class LettuceScanIteration<T> extends ScanIteration<T> {
        private final io.lettuce.core.ScanCursor cursor;

        LettuceScanIteration(io.lettuce.core.ScanCursor cursor, Collection<T> items) {
            super(Long.parseLong(cursor.getCursor()), items);
            this.cursor = cursor;
        }
    }
}
