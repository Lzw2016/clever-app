package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.util.NumberUtils;

/**
 * 将字符转换为任何JDK标准数字实现<br/>
 * 支持数字类，包括Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal。<br/>
 * 委托给{@link NumberUtils#convertNumberToTargetClass(Number, Class)}来执行转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:05 <br/>
 */
final class CharacterToNumberFactory implements ConverterFactory<Character, Number> {
    @Override
    public <T extends Number> Converter<Character, T> getConverter(Class<T> targetType) {
        return new CharacterToNumber<>(targetType);
    }

    private static final class CharacterToNumber<T extends Number> implements Converter<Character, T> {
        private final Class<T> targetType;

        public CharacterToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public T convert(Character source) {
            return NumberUtils.convertNumberToTargetClass((short) source.charValue(), this.targetType);
        }
    }
}
