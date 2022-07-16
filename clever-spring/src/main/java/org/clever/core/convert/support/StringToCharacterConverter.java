package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

/**
 * 将字符串转换为字符
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:03 <br/>
 */
final class StringToCharacterConverter implements Converter<String, Character> {
    @Override
    public Character convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        if (source.length() > 1) {
            throw new IllegalArgumentException(
                    "Can only convert a [String] with length of 1 to a [Character]; string value '" + source + "'  has length of " + source.length()
            );
        }
        return source.charAt(0);
    }
}
