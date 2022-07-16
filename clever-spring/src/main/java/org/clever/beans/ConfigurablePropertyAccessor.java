package org.clever.beans;

import org.clever.core.convert.ConversionService;

/**
 * 封装PropertyAccessor的配置方法的接口。
 * 还扩展了PropertyEditorRegistry接口，该接口定义了PropertyEditor管理的方法。
 * 用作{@link BeanWrapper}的基本接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:04 <br/>
 *
 * @see BeanWrapper
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {
    /**
     * 指定用于转换属性值的ConversionService，作为JavaBeans属性编辑器的替代
     */
    void setConversionService(ConversionService conversionService);

    /**
     * 返回关联的ConversionService(如果有)
     */
    ConversionService getConversionService();

    /**
     * 设置在将特性编辑器应用于特性的新值时，是否提取旧特性值
     */
    void setExtractOldValueForEditor(boolean extractOldValueForEditor);

    /**
     * 返回将属性编辑器应用于属性的新值时，是否提取旧属性值
     */
    boolean isExtractOldValueForEditor();

    /**
     * 设置此实例是否应尝试“自动增长”包含空值的嵌套路径<br/>
     * 如果为true，则将使用默认对象值填充并遍历空路径位置，而不是导致{@link NullValueInNestedPathException}<br/>
     * 对于普通PropertyAccessor实例，默认值为false
     */
    void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

    /**
     * 返回嵌套路径的“自动增长”是否已激活
     */
    boolean isAutoGrowNestedPaths();
}
