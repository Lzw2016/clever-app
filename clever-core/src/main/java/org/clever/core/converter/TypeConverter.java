package org.clever.core.converter;

/**
 * 自定义不同类型的对象转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2019/12/19 10:40 <br/>
 */
public interface TypeConverter {
    /**
     * 判断是否支持转换
     *
     * @param source     source 对象
     * @param targetType target 对象类型
     */
    boolean support(Object source, Class<?> targetType);

    /**
     * 将 source 对象转换为 target 对象
     *
     * @param source     source 对象
     * @param targetType target 对象类型
     * @param context    Converter context 对象
     * @return target 对象
     */
    Object convert(Object source, Class<?> targetType, Object context);
}
