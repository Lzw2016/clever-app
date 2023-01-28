package org.clever.data.util;

import org.clever.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 简化 {@link Iterable} 流式处理的简单接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:23 <br/>
 */
@FunctionalInterface
public interface Streamable<T> extends Iterable<T>, Supplier<Stream<T>> {
    /**
     * 返回一个空的 {@link Streamable}
     *
     * @return 永远不会是 {@literal null}
     */
    static <T> Streamable<T> empty() {
        return Collections::emptyIterator;
    }

    /**
     * 返回具有给定元素的 {@link Streamable}
     *
     * @param t 要返回的元素
     */
    @SafeVarargs
    static <T> Streamable<T> of(T... t) {
        return () -> Arrays.asList(t).iterator();
    }

    /**
     * 为给定的 {@link Iterable} 返回一个 {@link Streamable}
     *
     * @param iterable 不能是 {@literal null}
     */
    static <T> Streamable<T> of(Iterable<T> iterable) {
        Assert.notNull(iterable, "Iterable must not be null");
        return iterable::iterator;
    }

    static <T> Streamable<T> of(Supplier<? extends Stream<T>> supplier) {
        return LazyStreamable.of(supplier);
    }

    /**
     * 创建底层 {@link Iterable} 的非并行 {@link Stream}
     *
     * @return 永远不会是 {@literal null}
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回一个新的 {@link Streamable}，它将给定的 {@link Function} 应用到当前的
     *
     * @param mapper 不得为 {@literal null}
     * @see Stream#map(Function)
     */
    default <R> Streamable<R> map(Function<? super T, ? extends R> mapper) {
        Assert.notNull(mapper, "Mapping function must not be null");
        return Streamable.of(() -> stream().map(mapper));
    }

    /**
     * 返回一个新的 {@link Streamable}，它将给定的 {@link Function} 应用到当前的
     *
     * @param mapper 不得为 {@literal null}
     * @see Stream#flatMap(Function)
     */
    default <R> Streamable<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        Assert.notNull(mapper, "Mapping function must not be null");
        return Streamable.of(() -> stream().flatMap(mapper));
    }

    /**
     * 返回一个新的 {@link Streamable}，它将把给定的过滤器 {@link Predicate} 应用于当前过滤器
     *
     * @param predicate 不得为 {@literal null}
     * @see Stream#filter(Predicate)
     */
    default Streamable<T> filter(Predicate<? super T> predicate) {
        Assert.notNull(predicate, "Filter predicate must not be null");
        return Streamable.of(() -> stream().filter(predicate));
    }

    /**
     * 返回当前 {@link Streamable} 是否为空
     */
    default boolean isEmpty() {
        return !iterator().hasNext();
    }

    /**
     * 从当前的 {@link Streamable} 和给定的 {@link Stream} 连接创建一个新的 {@link Streamable}
     *
     * @param stream 不得为 {@literal null}
     */
    default Streamable<T> and(Supplier<? extends Stream<? extends T>> stream) {
        Assert.notNull(stream, "Stream must not be null");
        return Streamable.of(() -> Stream.concat(this.stream(), stream.get()));
    }

    /**
     * 从当前的 {@link Streamable} 和给定的值连接创建一个新的
     *
     * @param others 不得为 {@literal null}
     * @return 永远不会是{@literal null}
     */
    @SuppressWarnings("unchecked")
    default Streamable<T> and(T... others) {
        Assert.notNull(others, "Other values must not be null");
        return Streamable.of(() -> Stream.concat(this.stream(), Arrays.stream(others)));
    }

    /**
     * 从当前的 {@link Streamable} 和给定的 {@link Iterable} 连接创建一个新的 {@link Streamable}
     *
     * @param iterable 不得为 {@literal null}
     * @return 永远不会是 {@literal null}
     */
    default Streamable<T> and(Iterable<? extends T> iterable) {
        Assert.notNull(iterable, "Iterable must not be null");
        return Streamable.of(() -> Stream.concat(this.stream(), StreamSupport.stream(iterable.spliterator(), false)));
    }

    /**
     * 允许直接添加 {@link Streamable} 的便捷方法，否则 {@link #and(Iterable)} 和 {@link #and(Supplier)} 之间的调用不明确
     *
     * @param streamable 不得为 {@literal null}
     * @return 永远不会是 {@literal null}
     */
    default Streamable<T> and(Streamable<? extends T> streamable) {
        return and((Supplier<? extends Stream<? extends T>>) streamable);
    }

    /**
     * 创建一个新的、不可修改的 {@link List}
     *
     * @return 永远不会是 {@literal null}
     */
    default List<T> toList() {
        return stream().collect(StreamUtils.toUnmodifiableList());
    }

    /**
     * 创建一个新的、不可修改的 {@link Set}
     *
     * @return 永远不会是 {@literal null}
     */
    default Set<T> toSet() {
        return stream().collect(StreamUtils.toUnmodifiableSet());
    }

    default Stream<T> get() {
        return stream();
    }

    /**
     * 一个收集器，使用 {@link Collectors#toList} 作为中间收集器，从 {@link Stream} 轻松生成 {@link Streamable}
     *
     * @see #toStreamable(Collector)
     */
    static <S> Collector<S, ?, Streamable<S>> toStreamable() {
        return toStreamable(Collectors.toList());
    }

    /**
     * 一个收集器，可以从 {@link Stream} 和给定的中间收集器轻松生成 {@link Streamable}
     */
    @SuppressWarnings("unchecked")
    static <S, T extends Iterable<S>> Collector<S, ?, Streamable<S>> toStreamable(Collector<S, ?, T> intermediate) {
        return Collector.of(
                (Supplier<T>) intermediate.supplier(),
                (BiConsumer<T, S>) intermediate.accumulator(),
                (BinaryOperator<T>) intermediate.combiner(),
                Streamable::of
        );
    }
}
