package org.clever.core.convert.converter;

/**
 * 用于向类型转换系统注册转换器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:59 <br/>
 */
public interface ConverterRegistry {
    /**
     * 注册一个1:1的转换器<br/>
     * 会尝试去参数化类型中提取sourceType和targetType
     *
     * @throws IllegalArgumentException 如果无法解析参数化类型
     */
    void addConverter(Converter<?, ?> converter);

    /**
     * 注册一个1:1的转换器<br/>
     * 明确sourceType和targetType
     */
    <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

    /**
     * 注册一个N:N的转换器
     */
    void addConverter(GenericConverter converter);

    /**
     * 注册一个1:N的转化器<br/>
     * 会尝试去参数化类型中提取sourceType和targetType
     *
     * @throws IllegalArgumentException 如果无法解析参数化类型
     */
    void addConverterFactory(ConverterFactory<?, ?> factory);

    /**
     * 删除从{@code sourceType}到{@code targetType}的所有转换器
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     */
    void removeConvertible(Class<?> sourceType, Class<?> targetType);
}
