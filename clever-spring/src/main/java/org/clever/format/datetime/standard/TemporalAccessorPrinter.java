package org.clever.format.datetime.standard;

import org.clever.format.Printer;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * JSR-310 {@link java.time.temporal.TemporalAccessor}的格式化实现，使用{@link java.time.format.DateTimeFormatter}(上下文中如果有一个)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:28 <br/>
 */
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {
    private final DateTimeFormatter formatter;

    public TemporalAccessorPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(TemporalAccessor partial, Locale locale) {
        return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
    }
}
