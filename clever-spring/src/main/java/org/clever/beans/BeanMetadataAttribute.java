package org.clever.beans;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 作为bean定义一部分的键值样式属性的持有者。
 * 除了键值对外，还跟踪定义源。
 *
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:12 <br/>
 */
public class BeanMetadataAttribute implements BeanMetadataElement {
    private final String name;
    private final Object value;
    private Object source;

    /**
     * 创建新的AttributeValue实例。
     * @param name 属性的名称（从不为null）
     * @param value 属性的值（可能在类型转换之前）
     */
    public BeanMetadataAttribute(String name,  Object value) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
    }

    /**
     * 返回属性的名称。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回属性的值。
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * 为此元数据元素设置配置源 {@code Object}
     * <p>对象的确切类型将取决于所使用的配置机制
     */
    public void setSource( Object source) {
        this.source = source;
    }

    @Override
    public Object getSource() {
        return this.source;
    }

    @Override
    public boolean equals( Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanMetadataAttribute)) {
            return false;
        }
        BeanMetadataAttribute otherMa = (BeanMetadataAttribute) other;
        return (this.name.equals(otherMa.name)
                && ObjectUtils.nullSafeEquals(this.value, otherMa.value)
                && ObjectUtils.nullSafeEquals(this.source, otherMa.source));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "metadata attribute '" + this.name + "'";
    }
}
