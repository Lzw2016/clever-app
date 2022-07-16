package org.clever.beans;

import java.beans.PropertyDescriptor;

/**
 * 底层JavaBeans基础架构的中央接口<br/>
 * 提供用于分析和操作标准JavaBeans的操作：能够获取和设置属性值(单独或批量)、获取属性描述符以及查询属性的可读写性<br/>
 * 此接口支持嵌套属性，可以将子属性的属性设置为无限深度<br/>
 * BeanWrapper的“extractOldValueForEditor”设置的默认值为“false”，以避免getter方法调用引起的副作用。
 * 将此设置为“true”可向自定义编辑器公开当前属性值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:05 <br/>
 *
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {
    /**
     * 指定数组和集合自动增长的限制。普通BeanWrapper上的默认值是无限制的
     */
    void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

    /**
     * 返回数组和集合自动增长的限制
     */
    int getAutoGrowCollectionLimit();

    /**
     * 返回此对象包装的bean实例
     */
    Object getWrappedInstance();

    /**
     * 返回包装bean实例的类型
     */
    Class<?> getWrappedClass();

    /**
     * 获取包装对象的PropertyDescriptors(由标准JavaBeans内省确定)
     */
    PropertyDescriptor[] getPropertyDescriptors();

    /**
     * 获取包装对象的特定属性的属性描述符
     *
     * @param propertyName 要获取描述符的属性（可以是嵌套路径，但没有索引/映射属性）
     * @throws InvalidPropertyException 如果没有此类属性
     */
    PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;
}
