package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;
import org.clever.util.StringUtils;

import java.util.TimeZone;

/**
 * 将字符串转换为 {@link TimeZone}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:32 <br/>
 */
class StringToTimeZoneConverter implements Converter<String, TimeZone> {
    @Override
    public TimeZone convert(String source) {
        return StringUtils.parseTimeZoneString(source);
    }
}
