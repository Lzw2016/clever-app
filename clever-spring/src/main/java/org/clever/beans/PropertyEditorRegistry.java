package org.clever.beans;

import java.beans.PropertyEditor;

/**
 * JavaBeans属性编辑器注册表
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 15:57 <br/>
 *
 * @see java.beans.PropertyEditor
 * @see BeanWrapper
 */
public interface PropertyEditorRegistry {
    /**
     * 为指定的类型注册编辑器
     *
     * @param requiredType   属性的类型
     * @param propertyEditor 注册的编辑器
     */
    void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

    /**
     * 注册给定的自定义属性编辑器
     *
     * @param requiredType   属性的类型
     * @param propertyPath   属性的路径(名称或嵌套路径)
     * @param propertyEditor 注册的编辑器
     */
    void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor);

    /**
     * 查找指定类型的编辑器
     *
     * @param requiredType 属性的类型
     * @param propertyPath 属性的路径(名称或嵌套路径)
     */
    PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath);
}
