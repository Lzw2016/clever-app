package org.clever.data.redis.core;

import org.clever.data.util.CloseableIterator;

/**
 * 使用 {@code SCAN} 命令的变体扫描数据结构中的键空间或元素的游标抽象。
 * <p>
 * 使用 Java 8 {@link #stream() java.util.stream.Stream} 允许应用额外的
 * {@link java.util.stream.Stream#filter(java.util.function.Predicate) 过滤器} 和 {@link java.util.stream.Stream#limit(long) 限制}
 * 到底层 {@link org.clever.data.redis.core.Cursor}。
 * <p>
 * 确保在完成后{@link org.clever.data.util.CloseableIterator#close() 关闭}游标，
 * 因为这允许实现清理他们需要保持打开以迭代元素的任何资源(例如，通过使用 try-with-resource 声明)。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:39 <br/>
 */
public interface Cursor<T> extends CloseableIterator<T> {
    /**
     * Get the reference cursor. <br>
     * <strong>NOTE:</strong> the id might change while iterating items.
     */
    long getCursorId();

    /**
     * @return {@code true} if cursor closed.
     */
    boolean isClosed();

    /**
     * 打开游标并返回自身。此方法旨在由构建 {@link Cursor} 的组件调用，不应在外部调用。
     *
     * @return 打开的游标
     * @deprecated 将在下一个主要版本中删除
     */
    @Deprecated
    Cursor<T> open();

    /**
     * @return 光标的当前位置
     */
    long getPosition();
}
