package org.clever.format.datetime.joda;

import org.clever.format.Parser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * 使用{@link org.joda.time.format.DateTimeFormatter}解析{@link org.joda.time.LocalDate}实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:54 <br/>
 */
public final class LocalDateParser implements Parser<LocalDate> {
    private final DateTimeFormatter formatter;

    public LocalDateParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LocalDate parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDate(text);
    }
}

