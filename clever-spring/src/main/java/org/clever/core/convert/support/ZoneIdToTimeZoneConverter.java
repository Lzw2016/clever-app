package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Java8的{@link java.time.ZoneId}装换为{@link java.util.TimeZone}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:33 <br/>
 */
final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {
    @Override
    public TimeZone convert(ZoneId source) {
        return TimeZone.getTimeZone(source);
    }
}
