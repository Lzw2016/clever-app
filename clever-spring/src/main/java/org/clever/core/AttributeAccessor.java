package org.clever.core;

import org.clever.util.Assert;

import java.util.function.Function;

/**
 * 定义用于附加和访问任意对象元数据的通用约定的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:09 <br/>
 */
public interface AttributeAccessor {
    /**
     * 将名称定义的属性设置为提供的值 <br/>
     * 如果值为null，则删除该属性 <br/>
     * 一般来说，用户应该注意使用完全限定名，可能使用类名或包名作为前缀，以防止与其他元数据属性重叠
     *
     * @param name  唯一属性键
     * @param value 属性值
     */
    void setAttribute(String name, Object value);

    /**
     * 获取由名称标识的属性的值。如果属性不存在，则返回null
     *
     * @param name 唯一属性键
     * @return 属性的当前值(如果有)
     */
    Object getAttribute(String name);

    /**
     * 如有必要，计算由名称标识的属性的新值，并在此AttributeAccessor中设置新值 <br/>
     * 如果此AttributeAccessor中已存在由name标识的属性的值，则将返回现有值，而不应用提供的计算函数 <br/>
     * 此方法的默认实现不是线程安全的，但可以被此接口的具体实现覆盖
     *
     * @param <T>             属性值的类型
     * @param name            唯一属性键
     * @param computeFunction 为属性名称计算新值的函数；函数不能返回null值
     * @return 命名属性的现有值或新计算的值
     * @see #getAttribute(String)
     * @see #setAttribute(String, Object)
     */
    @SuppressWarnings("unchecked")
    default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(computeFunction, "Compute function must not be null");
        Object value = getAttribute(name);
        if (value == null) {
            value = computeFunction.apply(name);
            Assert.state(value != null, () -> String.format("Compute function must not return null for attribute named '%s'", name));
            setAttribute(name, value);
        }
        return (T) value;
    }

    /**
     * 删除由名称标识的属性并返回其值。
     * 如果在name下找不到属性，则返回null
     *
     * @param name 唯一属性键
     * @return 被移除的属性值
     */
    Object removeAttribute(String name);

    /**
     * 如果按名称标识的属性存在，则返回true。否则返回false
     *
     * @param name 唯一属性键
     */
    boolean hasAttribute(String name);

    /**
     * 返回所有属性的名称
     */
    String[] attributeNames();
}
