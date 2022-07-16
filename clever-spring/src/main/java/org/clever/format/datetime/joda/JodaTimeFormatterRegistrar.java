package org.clever.format.datetime.joda;

import org.clever.format.FormatterRegistrar;
import org.clever.format.FormatterRegistry;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.DateTimeFormat.ISO;
import org.clever.util.StringValueResolver;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * 配置Joda Time的格式化系统
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:40 <br/>
 */
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {
    private enum Type {DATE, TIME, DATE_TIME}

    /**
     * 用户定义的格式化程序
     */
    private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);
    /**
     * 未指定特定格式化程序时使用的工厂
     */
    private final Map<Type, DateTimeFormatterFactory> factories;

    private final StringValueResolver stringValueResolver;

    public JodaTimeFormatterRegistrar(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
        this.factories = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            this.factories.put(type, new DateTimeFormatterFactory());
        }
    }

    /**
     * 设置是否应将标准ISO格式应用于所有date/time类型。默认值为“false”。
     * 如果设置为“true”，则会有效忽略“dateStyle”、“timeStyle”和“dateTimeStyle”属性
     */
    @SuppressWarnings("DuplicatedCode")
    public void setUseIsoFormat(boolean useIsoFormat) {
        this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
        this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
        this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
    }

    /**
     * 设置Joda {@link LocalDate}对象的默认格式样式。默认值为{@link DateTimeFormat#shortDate()}
     */
    public void setDateStyle(String dateStyle) {
        this.factories.get(Type.DATE).setStyle(dateStyle + "-");
    }

    /**
     * 设置Joda {@link LocalTime}对象的默认格式样式。默认值为{@link DateTimeFormat#shortTime()}
     */
    public void setTimeStyle(String timeStyle) {
        this.factories.get(Type.TIME).setStyle("-" + timeStyle);
    }

    /**
     * 设置Joda {@link LocalDateTime}和{@link DateTime}对象以及JDK{@link Date}和{@link Calendar}对象的默认格式样式。默认值为{@link DateTimeFormat#shortDateTime()}
     */
    public void setDateTimeStyle(String dateTimeStyle) {
        this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
    }

    /**
     * 设置将用于表示日期值的对象的格式化程序。
     * 此格式化程序将用于{@link LocalDate}类型。指定时，将忽略{@link #setDateStyle(String)}和{@link #setUseIsoFormat(boolean)}属性
     *
     * @param formatter 要使用的格式化程序
     * @see #setTimeFormatter
     * @see #setDateTimeFormatter
     */
    public void setDateFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.DATE, formatter);
    }

    /**
     * 设置将用于表示日期值的对象的格式化程序。
     * 此格式化程序将用于{@link LocalTime}类型。指定时，将忽略{@link #setTimeStyle(String)}和{@link #setUseIsoFormat(boolean)}属性
     *
     * @param formatter 要使用的格式化程序
     * @see #setDateFormatter
     * @see #setDateTimeFormatter
     */
    public void setTimeFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.TIME, formatter);
    }

    /**
     * 设置将用于表示日期值的对象的格式化程序。
     * 此格式化程序将用于{@link LocalDateTime}, {@link ReadableInstant},{@link Date}和{@link Calendar}类型。
     * 指定时，将忽略{@link #setTimeStyle(String)}和{@link #setUseIsoFormat(boolean)}属性
     *
     * @param formatter 要使用的格式化程序
     * @see #setDateFormatter
     * @see #setTimeFormatter
     */
    public void setDateTimeFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.DATE_TIME, formatter);
    }

    @Override
    public void registerFormatters(FormatterRegistry registry) {
        JodaTimeConverters.registerConverters(registry);

        DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
        DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
        DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);

        addFormatterForFields(registry,
                new ReadablePartialPrinter(dateFormatter),
                new LocalDateParser(dateFormatter),
                LocalDate.class);

        addFormatterForFields(registry,
                new ReadablePartialPrinter(timeFormatter),
                new LocalTimeParser(timeFormatter),
                LocalTime.class);

        addFormatterForFields(registry,
                new ReadablePartialPrinter(dateTimeFormatter),
                new LocalDateTimeParser(dateTimeFormatter),
                LocalDateTime.class);

        addFormatterForFields(registry,
                new ReadableInstantPrinter(dateTimeFormatter),
                new DateTimeParser(dateTimeFormatter),
                ReadableInstant.class);

        // In order to retain backwards compatibility we only register Date/Calendar
        // types when a user defined formatter is specified (see SPR-10105)
        if (this.formatters.containsKey(Type.DATE_TIME)) {
            addFormatterForFields(registry,
                    new ReadableInstantPrinter(dateTimeFormatter),
                    new DateTimeParser(dateTimeFormatter),
                    Date.class, Calendar.class);
        }

        registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
        registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
        registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
        registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

        registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory(stringValueResolver));
    }

    private DateTimeFormatter getFormatter(Type type) {
        DateTimeFormatter formatter = this.formatters.get(type);
        if (formatter != null) {
            return formatter;
        }
        DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
        return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
    }

    private DateTimeFormatter getFallbackFormatter(Type type) {
        switch (type) {
            case DATE:
                return DateTimeFormat.shortDate();
            case TIME:
                return DateTimeFormat.shortTime();
            default:
                return DateTimeFormat.shortDateTime();
        }
    }

    private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer, Parser<?> parser, Class<?>... fieldTypes) {
        for (Class<?> fieldType : fieldTypes) {
            registry.addFormatterForFieldType(fieldType, printer, parser);
        }
    }
}
