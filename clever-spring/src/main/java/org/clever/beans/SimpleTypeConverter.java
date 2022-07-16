package org.clever.beans;

/**
 * {@link TypeConverter}接口的简单实现，不操作特定的目标对象。
 * 这是一种替代方法，用于满足任意类型转换需求，同时使用完全相同的转换算法
 * （包括对{@link java.beans.PropertyEditor}和{@link org.clever.core.convert.ConversionService}的委托）。
 *
 * <p>
 * 注意：由于它依赖于{@link java.beans.PropertyEditor PropertyEditors}，
 * SimpleTypeConverter不是线程安全的。每个线程使用一个单独的实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:25 <br/>
 *
 * @see BeanWrapperImpl
 */
public class SimpleTypeConverter extends TypeConverterSupport {
    public SimpleTypeConverter() {
        this.typeConverterDelegate = new TypeConverterDelegate(this);
        registerDefaultEditors();
    }
}
