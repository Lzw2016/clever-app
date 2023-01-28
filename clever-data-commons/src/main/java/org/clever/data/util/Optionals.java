package org.clever.data.util;

import org.clever.util.Assert;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * 使用 {@link Optional} 的实用方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 13:24 <br/>
 */
public interface Optionals {
    /**
     * 返回是否存在任何给定的 {@link Optional}
     *
     * @param optionals 不得为 {@literal null}
     */
    static boolean isAnyPresent(Optional<?>... optionals) {
        Assert.notNull(optionals, "Optionals must not be null");
        return Arrays.stream(optionals).anyMatch(Optional::isPresent);
    }

    /**
     * 将给定的 {@link Optional} 转换为单元素 {@link Stream} 或如果不存在则为空
     *
     * @param optionals 不得为 {@literal null}
     */
    @SafeVarargs
    static <T> Stream<T> toStream(Optional<? extends T>... optionals) {
        Assert.notNull(optionals, "Optional must not be null");
        return Arrays.stream(optionals).flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty));
    }

    /**
     * 将给定函数应用于源的元素并返回第一个非空结果
     *
     * @param source   不得为 {@literal null}
     * @param function 不得为 {@literal null}
     */
    static <S, T> Optional<T> firstNonEmpty(Iterable<S> source, Function<S, Optional<T>> function) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(function, "Function must not be null");
        return Streamable.of(source).stream().map(function).filter(Optional::isPresent).findFirst().orElseGet(Optional::empty);
    }

    /**
     * 将给定函数应用于源的元素并返回第一个非空结果
     *
     * @param source   不得为 {@literal null}
     * @param function 不得为 {@literal null}
     */
    static <S, T> T firstNonEmpty(Iterable<S> source, Function<S, T> function, T defaultValue) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(function, "Function must not be null");
        return Streamable.of(source).stream().map(function).filter(it -> !it.equals(defaultValue)).findFirst().orElse(defaultValue);
    }

    /**
     * 为 {@link Optional} 结果一个一个地调用给定的 {@link Supplier} 并返回第一个非空的
     *
     * @param suppliers 不得为 {@literal null}
     */
    @SafeVarargs
    static <T> Optional<T> firstNonEmpty(Supplier<Optional<T>>... suppliers) {
        Assert.notNull(suppliers, "Suppliers must not be null");
        return firstNonEmpty(Streamable.of(suppliers));
    }

    /**
     * 为 {@link Optional} 结果一个一个地调用给定的 {@link Supplier} 并返回第一个非空的
     *
     * @param suppliers 不得为 {@literal null}
     */
    static <T> Optional<T> firstNonEmpty(Iterable<Supplier<Optional<T>>> suppliers) {
        Assert.notNull(suppliers, "Suppliers must not be null");
        return Streamable.of(suppliers).stream().map(Supplier::get).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    /**
     * 如果没有下一个元素，则返回给定 {@link Iterator} 或 {@link Optional#empty()} 的下一个元素
     *
     * @param iterator 不得为 {@literal null}
     */
    static <T> Optional<T> next(Iterator<T> iterator) {
        Assert.notNull(iterator, "Iterator must not be null");
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    /**
     * 如果两个 {@link Optional} 实例都有值，则返回 {@link Pair} 或 {@link Optional#empty()} 如果一个或两个都缺失
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T, S> Optional<Pair<T, S>> withBoth(Optional<T> left, Optional<S> right) {
        return left.flatMap(l -> right.map(r -> Pair.of(l, r)));
    }

    /**
     * 如果所有给定的 {@link Optional} 都存在，则调用给定的 {@link BiConsumer}
     *
     * @param left     不得为 {@literal null}
     * @param right    不得为 {@literal null}
     * @param consumer 不得为 {@literal null}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T, S> void ifAllPresent(Optional<T> left, Optional<S> right, BiConsumer<T, S> consumer) {
        Assert.notNull(left, "Optional must not be null");
        Assert.notNull(right, "Optional must not be null");
        Assert.notNull(consumer, "Consumer must not be null");
        mapIfAllPresent(left, right, (l, r) -> {
            consumer.accept(l, r);
            return null;
        });
    }

    /**
     * 如果它们都存在，则映射给定 {@link Optional} 中包含的值
     *
     * @param left     不得为 {@literal null}
     * @param right    不得为 {@literal null}
     * @param function 不得为 {@literal null}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T, S, R> Optional<R> mapIfAllPresent(Optional<T> left, Optional<S> right, BiFunction<T, S, R> function) {
        Assert.notNull(left, "Optional must not be null");
        Assert.notNull(right, "Optional must not be null");
        Assert.notNull(function, "BiFunction must not be null");
        return left.flatMap(l -> right.map(r -> function.apply(l, r)));
    }

    /**
     * 如果 {@link Optional} 存在，则调用给定的 {@link Consumer}，如果不存在，则调用 {@link Runnable}
     *
     * @param optional 不得为 {@literal null}
     * @param consumer 不得为 {@literal null}
     * @param runnable 不得为 {@literal null}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> consumer, Runnable runnable) {
        Assert.notNull(optional, "Optional must not be null");
        Assert.notNull(consumer, "Consumer must not be null");
        Assert.notNull(runnable, "Runnable must not be null");
        if (optional.isPresent()) {
            optional.ifPresent(consumer);
        } else {
            runnable.run();
        }
    }
}
