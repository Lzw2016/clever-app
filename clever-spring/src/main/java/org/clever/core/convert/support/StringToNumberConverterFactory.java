package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.util.NumberUtils;

/**
 * 从字符串转换任何JDK标准数字实现。
 * 支持Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal。委托给{@link NumberUtils#parseNumber(String, Class)}来执行转换。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:17 <br/>
 */
final class StringToNumberConverterFactory implements ConverterFactory<String, Number> {
    @Override
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToNumber<>(targetType);
    }

    private static final class StringToNumber<T extends Number> implements Converter<String, T> {
        private final Class<T> targetType;

        public StringToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public T convert(String source) {
            if (source.isEmpty()) {
                return null;
            }
            return NumberUtils.parseNumber(source, this.targetType);
        }
    }
}
