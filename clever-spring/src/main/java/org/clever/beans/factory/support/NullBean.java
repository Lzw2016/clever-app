package org.clever.beans.factory.support;

/**
 * 空bean实例的内部表示
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 23:24 <br/>
 */
public final class NullBean {
    NullBean() {
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return (this == obj || obj == null);
    }

    @Override
    public int hashCode() {
        return NullBean.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }
}
