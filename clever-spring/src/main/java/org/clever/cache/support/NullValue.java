package org.clever.cache.support;

import java.io.Serializable;

/**
 * 简单的可序列化类，用作 {@code null} 替换不支持 {@code null} 值的缓存存储
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 17:02 <br/>
 */
public final class NullValue implements Serializable {
    public static final Object INSTANCE = new NullValue();
    private static final long serialVersionUID = 1L;

    private NullValue() {
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj || obj == null);
    }

    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }
}
