package org.clever.format.datetime.joda;

import org.clever.format.Printer;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * 使用{@link DateTimeFormatter}格式化Joda Time {@link ReadableInstant}实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:57 <br/>
 */
public final class ReadableInstantPrinter implements Printer<ReadableInstant> {
    private final DateTimeFormatter formatter;

    public ReadableInstantPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(ReadableInstant instant, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
    }
}
