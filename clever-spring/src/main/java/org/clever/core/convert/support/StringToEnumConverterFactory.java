package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;

/**
 * 通过调用{@link Enum#valueOf(Class, String)}将字符串转换为{@link java.lang.Enum}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:07 <br/>
 */
@SuppressWarnings({"ConvertToBasicLatin", "rawtypes", "unchecked"})
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {
    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnum(ConversionUtils.getEnumType(targetType));
    }

    @SuppressWarnings("unchecked")
    private static class StringToEnum<T extends Enum> implements Converter<String, T> {
        private final Class<T> enumType;

        StringToEnum(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source.isEmpty()) {
                // It's an empty enum identifier: reset the enum value to null.
                return null;
            }
            return (T) Enum.valueOf(this.enumType, source.trim());
        }
    }
}
