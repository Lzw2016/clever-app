package org.clever.data.util;

import org.clever.util.Assert;
import org.clever.util.MultiValueMap;

import java.util.*;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;

/**
 * Spring Data特定的Java {@link Stream} 实用程序方法和类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:25 <br/>
 */
public interface StreamUtils {
    /**
     * 返回由给定 {@link Iterator} 支持的 {@link Stream}
     *
     * @param iterator 不得为 {@literal null}
     */
    static <T> Stream<T> createStreamFromIterator(Iterator<T> iterator) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * 返回由给定 {@link CloseableIterator} 支持的 {@link Stream} ，并将对 {@link Stream#close（）} 的调用转发给迭代器。
     *
     * @param iterator 不得为 {@literal null}
     */
    static <T> Stream<T> createStreamFromIterator(CloseableIterator<T> iterator) {
        Assert.notNull(iterator, "Iterator must not be null");
        return createStreamFromIterator((Iterator<T>) iterator).onClose(iterator::close);
    }

    /**
     * 返回 {@link Collector} 以创建不可修改的 {@link List}
     *
     * @return 永远不会是 {@literal null}
     */
    static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return collectingAndThen(toList(), Collections::unmodifiableList);
    }

    /**
     * 返回一个 {@link Collector} 以创建一个不可修改的 {@link Set}
     *
     * @return 永远不会是 {@literal null}
     */
    static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return collectingAndThen(toSet(), Collections::unmodifiableSet);
    }

    /**
     * 返回 {@link Collector} 以创建 {@link MultiValueMap}
     *
     * @param keyFunction   {@link Function} 从 {@link java.util.stream.Stream} 的元素创建键
     * @param valueFunction {@link Function} 从 {@link java.util.stream.Stream} 的元素创建值
     */
    static <T, K, V> Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> toMultiMap(Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return MultiValueMapCollector.of(keyFunction, valueFunction);
    }

    /**
     * 为给定值创建一个新的 {@link Stream} ，如果该值为 {@literal null} ，则返回一个空 {@link Stream}
     *
     * @param source 可以是 {@literal null}
     * @return 如果给定值为 {@literal null}，则为给定值创建一个新的 {@link Stream} ，返回一个空 {@link Stream}
     */
    static <T> Stream<T> fromNullable(T source) {
        return source == null ? Stream.empty() : Stream.of(source);
    }

    /**
     * 使用给定的 {@link BiFunction} 压缩给定的 {@link Stream} <br/>
     * 生成的 {@link Stream} 将具有两个 {@link Stream} 中较短的一个的长度，当两个{@link Stream}中较短者耗尽时，缩短压缩
     *
     * @param left     不得为 {@literal null}
     * @param right    不得为 {@literal null}
     * @param combiner 不得为 {@literal null}
     */
    static <L, R, T> Stream<T> zip(Stream<L> left, Stream<R> right, BiFunction<L, R, T> combiner) {
        Assert.notNull(left, "Left stream must not be null");
        Assert.notNull(right, "Right must not be null");
        Assert.notNull(combiner, "Combiner must not be null");
        Spliterator<L> lefts = left.spliterator();
        Spliterator<R> rights = right.spliterator();
        long size = Long.min(lefts.estimateSize(), rights.estimateSize());
        int characteristics = lefts.characteristics() & rights.characteristics();
        boolean parallel = left.isParallel() || right.isParallel();
        return StreamSupport.stream(new AbstractSpliterator<T>(size, characteristics) {
            @Override
            @SuppressWarnings("null")
            public boolean tryAdvance(Consumer<? super T> action) {
                Sink<L> leftSink = new Sink<L>();
                Sink<R> rightSink = new Sink<R>();
                boolean leftAdvance = lefts.tryAdvance(leftSink);
                if (!leftAdvance) {
                    return false;
                }
                boolean rightAdvance = rights.tryAdvance(rightSink);
                if (!rightAdvance) {
                    return false;
                }
                action.accept(combiner.apply(leftSink.getValue(), rightSink.getValue()));
                return true;
            }
        }, parallel);
    }
}
