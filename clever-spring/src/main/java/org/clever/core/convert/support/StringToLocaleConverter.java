package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.util.StringUtils;

import java.util.Locale;

/**
 * 从字符串转换为区域{@link java.util.Locale}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:12 <br/>
 */
final class StringToLocaleConverter implements Converter<String, Locale> {
    @Override
    public Locale convert(String source) {
        return StringUtils.parseLocale(source);
    }
}
