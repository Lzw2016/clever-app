package org.clever.format.datetime.joda;

import org.clever.format.Parser;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * 使用{@link org.joda.time.format.DateTimeFormatter}解析{@link org.joda.time.LocalDateTime}实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:56 <br/>
 */
public final class LocalDateTimeParser implements Parser<LocalDateTime> {
    private final DateTimeFormatter formatter;

    public LocalDateTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LocalDateTime parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDateTime(text);
    }
}

