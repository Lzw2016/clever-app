package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.util.StringUtils;

import java.util.UUID;

/**
 * 从字符串转换为 {@link java.util.UUID}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:18 <br/>
 */
final class StringToUUIDConverter implements Converter<String, UUID> {
    @Override
    public UUID convert(String source) {
        return (StringUtils.hasText(source) ? UUID.fromString(source.trim()) : null);
    }
}
