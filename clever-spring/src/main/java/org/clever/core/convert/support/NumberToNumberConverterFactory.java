package org.clever.core.convert.support;

import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalConverter;
import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.util.NumberUtils;

/**
 * JDK标准数字转换为JDK标准数字，
 * 包括Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal。
 * 委托给{@link NumberUtils#convertNumberToTargetClass(Number, Class)}来执行转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 16:14 <br/>
 */
final class NumberToNumberConverterFactory implements ConverterFactory<Number, Number>, ConditionalConverter {
    @Override
    public <T extends Number> Converter<Number, T> getConverter(Class<T> targetType) {
        return new NumberToNumber<>(targetType);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return !sourceType.equals(targetType);
    }

    private static final class NumberToNumber<T extends Number> implements Converter<Number, T> {
        private final Class<T> targetType;

        NumberToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public T convert(Number source) {
            return NumberUtils.convertNumberToTargetClass(source, this.targetType);
        }
    }
}
