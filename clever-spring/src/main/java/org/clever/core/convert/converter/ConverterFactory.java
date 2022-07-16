package org.clever.core.convert.converter;

/**
 * “范围”转换器的工厂，可以将对象从 {@code S} 转换为 {@code R} 的子类型 (1:N的转换器)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:02 <br/>
 */
public interface ConverterFactory<S, R> {
    /**
     * 获取一个将 S 转换为 T 的转换器，T 是 R 的子类型
     *
     * @param <T>        目标类型
     * @param targetType 要转换为的目标类型
     * @return a converter from S to T
     */
    <T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
