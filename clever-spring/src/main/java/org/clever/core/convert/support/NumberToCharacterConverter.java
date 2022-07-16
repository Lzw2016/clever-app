package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

/**
 * 将任何JDK标准数字实现转换为字符
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:05 <br/>
 */
final class NumberToCharacterConverter implements Converter<Number, Character> {
    @Override
    public Character convert(Number source) {
        return (char) source.shortValue();
    }
}
