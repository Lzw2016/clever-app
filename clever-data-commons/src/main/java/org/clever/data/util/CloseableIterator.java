package org.clever.data.util;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link CloseableIterator} 用作底层数据存储特定结果的桥接数据结构，这些结果可以包装在 Java 8 {@link #stream() java.util.stream.Stream} 中。<br/>
 * 这允许实现清理他们需要保持开放以迭代元素的任何资源。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 17:41 <br/>
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
    @Override
    void close();

    /**
     * 在此 {@link Iterator} 提供的元素上创建一个 {@link Spliterator}。<br/>
     * 实现应该记录拆分器报告的特征值。<br/>
     * 如果拆分器报告 {@link Spliterator#SIZED} 并且此集合不包含任何元素，则不需要报告此类特征值。
     * <p>
     * 默认实现应该被可以返回更高效的拆分器的子类覆盖。<br/>
     * 为了保留 {@link #stream()} 方法的预期惰性行为，拆分器应该具有 {@code IMMUTABLE} 或 {@code CONCURRENT} 的特征，或者是后期绑定。
     * <p>
     * 默认实现不报告大小。
     *
     * @return a {@link Spliterator} over the elements in this {@link Iterator}.
     */
    default Spliterator<T> spliterator() {
        return new IteratorSpliterator<>(this);
    }

    /**
     * 返回以此 {@link Iterator} 作为源的顺序 {@code Stream}。当{@link Stream#close() 关闭}时，生成的流调用{@link #close()}。<br/>
     * 生成的 {@link Stream} 在使用后必须关闭，它可以在 {@code try-with-resources} 语句中声明为资源。
     * <p>
     * 当 {@link #spliterator()} 方法无法返回 {@code IMMUTABLE}、{@code CONCURRENT} 或 <em>后期绑定<em> 的拆分器时，应覆盖此方法。(有关详细信息，请参阅 {@link #spliterator()})
     *
     * @return 此 {@link Iterator} 中元素的顺序 {@code Stream}
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(this::close);
    }
}
