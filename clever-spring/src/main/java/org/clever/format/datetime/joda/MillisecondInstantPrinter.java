package org.clever.format.datetime.joda;

import org.clever.format.Printer;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * 使用Joda {@link DateTimeFormatter}打印Long实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 21:01 <br/>
 */
public final class MillisecondInstantPrinter implements Printer<Long> {
    private final DateTimeFormatter formatter;

    public MillisecondInstantPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(Long instant, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
    }
}
