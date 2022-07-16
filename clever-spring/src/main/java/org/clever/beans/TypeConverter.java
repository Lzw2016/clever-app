package org.clever.beans;

import org.clever.core.MethodParameter;
import org.clever.core.convert.TypeDescriptor;

import java.lang.reflect.Field;

/**
 * 定义类型转换方法的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 15:58 <br/>
 *
 * @see BeanWrapperImpl
 */
public interface TypeConverter {
    /**
     * 将值转换为所需的类型(如果需要，从字符串转换)<br/>
     * 从字符串到任何类型的转换通常将使用PropertyEditor类的{@code setAsText}方法，或ConversionService中的转换器
     *
     * @param value        要转换的值
     * @param requiredType 我们必须转换为的类型(如果未知，则为null，例如在集合元素的情况下)
     * @throws TypeMismatchException 如果类型转换失败
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.clever.core.convert.ConversionService
     * @see org.clever.core.convert.converter.Converter
     */
    <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException;

    /**
     * 将值转换为所需的类型(如果需要，从字符串转换)<br/>
     * 从字符串到任何类型的转换通常将使用PropertyEditor类的{@code setAsText}方法，或ConversionService中的转换器
     *
     * @param value        要转换的值
     * @param requiredType 我们必须转换为的类型(如果未知，则为null，例如在集合元素的情况下)
     * @param methodParam  作为转换目标的方法参数(用于分析泛型类型；可以为null)
     * @throws TypeMismatchException 如果类型转换失败
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.clever.core.convert.ConversionService
     * @see org.clever.core.convert.converter.Converter
     */
    <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam) throws TypeMismatchException;

    /**
     * 将值转换为所需的类型(如果需要，从字符串转换)<br/>
     * 从字符串到任何类型的转换通常将使用PropertyEditor类的{@code setAsText}方法，或ConversionService中的转换器
     *
     * @param value        要转换的值
     * @param requiredType 我们必须转换为的类型(如果未知，则为null，例如在集合元素的情况下)
     * @param field        作为转换目标的反射字段(用于分析泛型类型；可以为null)
     * @throws TypeMismatchException 如果类型转换失败
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.clever.core.convert.ConversionService
     * @see org.clever.core.convert.converter.Converter
     */
    <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field) throws TypeMismatchException;

    /**
     * 将值转换为所需的类型(如果需要，从字符串转换)<br/>
     * 从字符串到任何类型的转换通常将使用PropertyEditor类的{@code setAsText}方法，或ConversionService中的转换器
     *
     * @param value          要转换的值
     * @param requiredType   我们必须转换为的类型(如果未知，则为null，例如在集合元素的情况下)
     * @param typeDescriptor 要使用的类型描述符(可能为空)
     * @throws TypeMismatchException 如果类型转换失败
     * @see java.beans.PropertyEditor#setAsText(String)
     * @see java.beans.PropertyEditor#getValue()
     * @see org.clever.core.convert.ConversionService
     * @see org.clever.core.convert.converter.Converter
     */
    default <T> T convertIfNecessary(Object value, Class<T> requiredType, TypeDescriptor typeDescriptor) throws TypeMismatchException {
        throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
    }
}
