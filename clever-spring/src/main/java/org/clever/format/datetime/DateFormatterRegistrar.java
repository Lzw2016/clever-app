package org.clever.format.datetime;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterRegistry;
import org.clever.format.FormatterRegistrar;
import org.clever.format.FormatterRegistry;
import org.clever.util.Assert;
import org.clever.util.StringValueResolver;

import java.util.Calendar;
import java.util.Date;

/**
 * 配置基本日期格式以用于{@link org.clever.format.annotation.DateTimeFormat}声明。
 * 适用于{@link Date}、{@link Calendar}和{@code long}类型的字段。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:09 <br/>
 */
public class DateFormatterRegistrar implements FormatterRegistrar {
    private final StringValueResolver stringValueResolver;
    private DateFormatter dateFormatter;

    public DateFormatterRegistrar(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    /**
     * 设置要注册的全局日期格式化程序。
     * 如果未指定，则不会注册非注解{@link Date}和{@link Calendar}字段的常规格式化程序
     */
    public void setFormatter(DateFormatter dateFormatter) {
        Assert.notNull(dateFormatter, "DateFormatter must not be null");
        this.dateFormatter = dateFormatter;
    }

    @Override
    public void registerFormatters(FormatterRegistry registry) {
        addDateConverters(registry);
        // In order to retain back compatibility we only register Date/Calendar
        // types when a user defined formatter is specified (see SPR-10105)
        if (this.dateFormatter != null) {
            registry.addFormatter(this.dateFormatter);
            registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
        }
        registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory(stringValueResolver));
    }

    /**
     * 将日期转换器添加到指定的注册表
     */
    public static void addDateConverters(ConverterRegistry converterRegistry) {
        converterRegistry.addConverter(new DateToLongConverter());
        converterRegistry.addConverter(new DateToCalendarConverter());
        converterRegistry.addConverter(new CalendarToDateConverter());
        converterRegistry.addConverter(new CalendarToLongConverter());
        converterRegistry.addConverter(new LongToDateConverter());
        converterRegistry.addConverter(new LongToCalendarConverter());
    }

    private static class DateToLongConverter implements Converter<Date, Long> {
        @Override
        public Long convert(Date source) {
            return source.getTime();
        }
    }

    private static class DateToCalendarConverter implements Converter<Date, Calendar> {
        @Override
        public Calendar convert(Date source) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(source);
            return calendar;
        }
    }

    private static class CalendarToDateConverter implements Converter<Calendar, Date> {
        @Override
        public Date convert(Calendar source) {
            return source.getTime();
        }
    }

    private static class CalendarToLongConverter implements Converter<Calendar, Long> {
        @Override
        public Long convert(Calendar source) {
            return source.getTimeInMillis();
        }
    }

    private static class LongToDateConverter implements Converter<Long, Date> {
        @Override
        public Date convert(Long source) {
            return new Date(source);
        }
    }

    private static class LongToCalendarConverter implements Converter<Long, Calendar> {
        @Override
        public Calendar convert(Long source) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(source);
            return calendar;
        }
    }
}
