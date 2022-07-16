package org.clever.beans;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.Serializable;

/**
 * 对象来保存单个bean属性的信息和值。<br/>
 * 在这里使用对象，而不仅仅是将所有属性存储在由属性名称设置关键字的映射中，这样可以提供更大的灵活性，并能够以优化的方式处理索引属性等。<br/>
 * 请注意，该值不需要是最终所需的类型：{@link BeanWrapper}实现应该处理任何必要转换，因为该对象不知道它将应用于哪些对象<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:06 <br/>
 *
 * @see PropertyValues
 * @see BeanWrapper
 */
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {
    private final String name;
    private final Object value;
    private boolean optional = false;
    private boolean converted = false;
    private Object convertedValue;
    /**
     * 包可见字段，指示是否需要转换
     */
    volatile Boolean conversionNecessary;
    /**
     * 用于缓存已解析属性路径标记的包可见字段
     */
    transient volatile Object resolvedTokens;

    /**
     * 创建新的PropertyValue实例
     *
     * @param name  属性的名称(从不为null)
     * @param value 属性的值(可能在类型转换之前)
     */
    public PropertyValue(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
    }

    /**
     * 复制构造函数
     *
     * @param original 要复制的PropertyValue(从不为null)
     */
    public PropertyValue(PropertyValue original) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = original.getValue();
        this.optional = original.isOptional();
        this.converted = original.converted;
        this.convertedValue = original.convertedValue;
        this.conversionNecessary = original.conversionNecessary;
        this.resolvedTokens = original.resolvedTokens;
        setSource(original.getSource());
        copyAttributesFrom(original);
    }

    /**
     * 为原始值持有者公开新值的构造函数。原始支架将作为新支架的来源公开
     *
     * @param original 要复制的PropertyValue(从不为null)
     * @param newValue 要应用的新值
     */
    public PropertyValue(PropertyValue original, Object newValue) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = newValue;
        this.optional = original.isOptional();
        this.conversionNecessary = original.conversionNecessary;
        this.resolvedTokens = original.resolvedTokens;
        setSource(original);
        copyAttributesFrom(original);
    }

    /**
     * 返回属性的名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回属性的值<br/>
     * 请注意，此处不会发生类型转换。BeanWrapper实现负责执行类型转换
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * 返回此值持有者的原始PropertyValue实例
     *
     * @return 原始PropertyValue(此值持有者的来源或此值持有者本身)
     */
    public PropertyValue getOriginalPropertyValue() {
        PropertyValue original = this;
        Object source = getSource();
        while (source instanceof PropertyValue && source != original) {
            original = (PropertyValue) source;
            source = original.getSource();
        }
        return original;
    }

    /**
     * 设置此值是否为可选值，即当目标类上不存在相应的属性时将被忽略
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * 返回此值是否为可选值，即当目标类上不存在相应的属性时将被忽略
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * 返回此持有者是否已包含已转换的值(true)，或该值是否仍需要转换(false)
     */
    public synchronized boolean isConverted() {
        return this.converted;
    }

    /**
     * 在处理类型转换后，设置此属性值的转换值
     */
    public synchronized void setConvertedValue(Object value) {
        this.converted = true;
        this.convertedValue = value;
    }

    /**
     * 在处理类型转换后，返回此属性值的转换值
     */
    public synchronized Object getConvertedValue() {
        return this.convertedValue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyValue)) {
            return false;
        }
        PropertyValue otherPv = (PropertyValue) other;
        return this.name.equals(otherPv.name)
                && ObjectUtils.nullSafeEquals(this.value, otherPv.value)
                && ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "bean property '" + this.name + "'";
    }
}
