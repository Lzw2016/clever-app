package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;

/**
 * 通过调用{@link Class#getEnumConstants()}将整数转换为{@link java.lang.Enum}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:10 <br/>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {
    @Override
    public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
        return new IntegerToEnum(ConversionUtils.getEnumType(targetType));
    }

    private static class IntegerToEnum<T extends Enum> implements Converter<Integer, T> {
        private final Class<T> enumType;

        public IntegerToEnum(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(Integer source) {
            return this.enumType.getEnumConstants()[source];
        }
    }
}
