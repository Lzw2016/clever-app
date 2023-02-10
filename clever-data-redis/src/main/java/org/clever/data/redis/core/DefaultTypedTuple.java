package org.clever.data.redis.core;

import org.clever.data.redis.core.ZSetOperations.TypedTuple;

import java.util.Arrays;

/**
 * TypedTuple 的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 15:45 <br/>
 */
public class DefaultTypedTuple<V> implements TypedTuple<V> {
    private final Double score;
    private final V value;

    /**
     * 构造一个新的<code>DefaultTypedTouple</code>实例
     *
     * @param value 可以是 {@literal null}。
     * @param score 可以是 {@literal null}。
     */
    public DefaultTypedTuple(V value, Double score) {
        this.score = score;
        this.value = value;
    }

    public Double getScore() {
        return score;
    }

    public V getValue() {
        return value;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((score == null) ? 0 : score.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DefaultTypedTuple))
            return false;
        DefaultTypedTuple<?> other = (DefaultTypedTuple<?>) obj;
        if (score == null) {
            if (other.score != null)
                return false;
        } else if (!score.equals(other.score))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (value instanceof byte[]) {
            if (!(other.value instanceof byte[])) {
                return false;
            }
            return Arrays.equals((byte[]) value, (byte[]) other.value);
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public int compareTo(Double o) {
        double thisScore = (score == null ? 0.0 : score);
        double otherScore = (o == null ? 0.0 : o);
        return Double.compare(thisScore, otherScore);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(TypedTuple<V> o) {
        if (o == null) {
            return compareTo(Double.valueOf(0));
        }
        return compareTo(o.getScore());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [score=" + score + ", value=" + value + ']';
    }
}
