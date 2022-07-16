package org.clever.core.convert.converter;

import org.clever.core.convert.TypeDescriptor;

/**
 * 允许{@link Converter}、{@link ConverterFactory}、{@link GenericConverter}增加一个转换判断条件<br/>
 * 转换判断条件基于: {@code source}、{@code target}以及{@link TypeDescriptor}的信息<br/>
 * 通常用于根据字段或类特征(如注解或函数)来匹配转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:15 <br/>
 */
public interface ConditionalConverter {
    /**
     * 当前转换器是否支持从源TypeDescriptor转换为目标TypeDescriptor
     *
     * @param sourceType 源TypeDescriptor
     * @param targetType 目标TypeDescriptor
     * @return 如果应执行转换，则为true，否则为false
     */
    boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);
}
