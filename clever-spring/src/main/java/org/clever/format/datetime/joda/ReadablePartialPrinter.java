package org.clever.format.datetime.joda;

import org.clever.format.Printer;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * 使用{@link DateTimeFormatter}格式化Joda Time {@link ReadablePartial}实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:48 <br/>
 */
public final class ReadablePartialPrinter implements Printer<ReadablePartial> {
    private final DateTimeFormatter formatter;

    public ReadablePartialPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(ReadablePartial partial, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(partial);
    }
}
