package org.clever.data.util;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 一个元组的东西
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 13:25 <br/>
 */
public final class Pair<S, T> {
    private final S first;
    private final T second;

    private Pair(S first, T second) {
        Assert.notNull(first, "First must not be null");
        Assert.notNull(second, "Second must not be null");
        this.first = first;
        this.second = second;
    }

    /**
     * 为给定的元素创建一个新的 {@link Pair}
     *
     * @param first  不得为 {@literal null}
     * @param second 不得为 {@literal null}
     */
    public static <S, T> Pair<S, T> of(S first, T second) {
        return new Pair<>(first, second);
    }

    /**
     * 返回 {@link Pair} 的第一个元素
     */
    public S getFirst() {
        return first;
    }

    /**
     * 返回 {@link Pair} 的第二个元素
     */
    public T getSecond() {
        return second;
    }

    /**
     * 从 {@link Pair} 的 {@link Stream} 创建 {@link Map} 的收集器
     */
    public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
        return Collectors.toMap(Pair::getFirst, Pair::getSecond);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) o;
        if (!ObjectUtils.nullSafeEquals(first, pair.first)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(second, pair.second);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(first);
        result = 31 * result + ObjectUtils.nullSafeHashCode(second);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s->%s", this.first, this.second);
    }
}
