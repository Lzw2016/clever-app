package org.clever.format.datetime.joda;

import org.clever.format.Parser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * 使用{@link DateTimeFormatter}解析Joda {@link DateTime}实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:57 <br/>
 */
public final class DateTimeParser implements Parser<DateTime> {
    private final DateTimeFormatter formatter;

    public DateTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public DateTime parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseDateTime(text);
    }
}

