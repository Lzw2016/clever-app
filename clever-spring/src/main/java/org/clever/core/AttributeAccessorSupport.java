package org.clever.core;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 支持{@link AttributeAccessor}类，提供所有方法的基本实现。由子类扩展。如果子类和所有属性值都可序列化，则可序列化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:09 <br/>
 */
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {
    /**
     * 使用字符串键和对象值进行映射
     */
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        if (value != null) {
            this.attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(computeFunction, "Compute function must not be null");
        Object value = this.attributes.computeIfAbsent(name, computeFunction);
        Assert.state(
                value != null,
                () -> String.format("Compute function must not return null for attribute named '%s'", name)
        );
        return (T) value;
    }

    @Override
    public Object removeAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.containsKey(name);
    }

    @Override
    public String[] attributeNames() {
        return StringUtils.toStringArray(this.attributes.keySet());
    }

    /**
     * 将提供的AttributeAccessor中的属性复制到此访问器
     *
     * @param source 要从中复制的AttributeAccessor
     */
    protected void copyAttributesFrom(AttributeAccessor source) {
        Assert.notNull(source, "Source must not be null");
        String[] attributeNames = source.attributeNames();
        for (String attributeName : attributeNames) {
            setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof AttributeAccessorSupport && this.attributes.equals(((AttributeAccessorSupport) other).attributes));
    }

    @Override
    public int hashCode() {
        return this.attributes.hashCode();
    }
}
