package org.clever.data.redis.connection;

import org.clever.data.redis.connection.RedisZSetCommands.Tuple;

import java.util.Arrays;

/**
 * {@link Tuple} 接口的默认实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:43 <br/>
 */
public class DefaultTuple implements Tuple {
    private final Double score;
    private final byte[] value;

    /**
     * 构造一个新的<code>默认元组</code>实例
     */
    public DefaultTuple(byte[] value, Double score) {
        this.score = score;
        this.value = value;
    }

    public Double getScore() {
        return score;
    }

    public byte[] getValue() {
        return value;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DefaultTuple))
            return false;
        DefaultTuple other = (DefaultTuple) obj;
        if (score == null) {
            if (other.score != null)
                return false;
        } else if (!score.equals(other.score))
            return false;
        // noinspection RedundantIfStatement
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((score == null) ? 0 : score.hashCode());
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(Double o) {
        Double d = (score == null ? Double.valueOf(0.0d) : score);
        Double a = (o == null ? Double.valueOf(0.0d) : o);
        return d.compareTo(a);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [score=" + score + ", value=" + (value == null ? "null" : new String(value)) + ']';
    }
}
