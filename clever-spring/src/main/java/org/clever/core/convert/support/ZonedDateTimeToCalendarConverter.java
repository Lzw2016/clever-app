package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Java8的{@link java.time.ZonedDateTime}装换为{@link java.util.Calendar}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:34 <br/>
 */
final class ZonedDateTimeToCalendarConverter implements Converter<ZonedDateTime, Calendar> {
    @Override
    public Calendar convert(ZonedDateTime source) {
        return GregorianCalendar.from(source);
    }
}
