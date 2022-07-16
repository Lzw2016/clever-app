package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * JSR-310 {@link java.time.Instant}的格式化程序实现，遵循JSR-310 {@link java.time.Instant}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:30 <br/>
 */
public class InstantFormatter implements Formatter<Instant> {
    @Override
    public Instant parse(String text, Locale locale) throws ParseException {
        if (text.length() > 0 && Character.isAlphabetic(text.charAt(0))) {
            // assuming RFC-1123 value a la "Tue, 3 Jun 2008 11:05:30 GMT"
            return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
        } else {
            // assuming UTC instant a la "2007-12-03T10:15:30.00Z"
            return Instant.parse(text);
        }
    }

    @Override
    public String print(Instant object, Locale locale) {
        return object.toString();
    }
}
