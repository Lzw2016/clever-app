package org.clever.data.convert;

import org.clever.core.convert.converter.Converter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;

/**
 * 一个帮助程序类，用于注册特定于 JSR-310 的 {@link Converter} 实现，以防我们在 Java 8 上运行。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:10 <br/>
 */
public abstract class Jsr310Converters {
    private static final List<Class<?>> CLASSES = Arrays.asList(
            LocalDateTime.class,
            LocalDate.class,
            LocalTime.class,
            Instant.class,
            ZoneId.class,
            Duration.class,
            Period.class
    );

    /**
     * 返回要注册的转换器。只有在我们在 Java 8 上运行的情况下才会返回转换器
     */
    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(DateToLocalDateTimeConverter.INSTANCE);
        converters.add(LocalDateTimeToDateConverter.INSTANCE);
        converters.add(DateToLocalDateConverter.INSTANCE);
        converters.add(LocalDateToDateConverter.INSTANCE);
        converters.add(DateToLocalTimeConverter.INSTANCE);
        converters.add(LocalTimeToDateConverter.INSTANCE);
        converters.add(DateToInstantConverter.INSTANCE);
        converters.add(InstantToDateConverter.INSTANCE);
        converters.add(LocalDateTimeToInstantConverter.INSTANCE);
        converters.add(InstantToLocalDateTimeConverter.INSTANCE);
        converters.add(ZoneIdToStringConverter.INSTANCE);
        converters.add(StringToZoneIdConverter.INSTANCE);
        converters.add(DurationToStringConverter.INSTANCE);
        converters.add(StringToDurationConverter.INSTANCE);
        converters.add(PeriodToStringConverter.INSTANCE);
        converters.add(StringToPeriodConverter.INSTANCE);
        converters.add(StringToLocalDateConverter.INSTANCE);
        converters.add(StringToLocalDateTimeConverter.INSTANCE);
        converters.add(StringToInstantConverter.INSTANCE);
        return converters;
    }

    public static boolean supports(Class<?> type) {
        return CLASSES.contains(type);
    }

    @ReadingConverter
    public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(Date source) {
            return ofInstant(source.toInstant(), systemDefault());
        }
    }

    @WritingConverter
    public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.atZone(systemDefault()).toInstant());
        }
    }

    @ReadingConverter
    public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(Date source) {
            return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
        }
    }

    @WritingConverter
    public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalDate source) {
            return Date.from(source.atStartOfDay(systemDefault()).toInstant());
        }
    }

    @ReadingConverter
    public enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {
        INSTANCE;

        @Override
        public LocalTime convert(Date source) {
            return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
        }
    }

    @WritingConverter
    public enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalTime source) {
            return Date.from(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
        }
    }

    @ReadingConverter
    public enum DateToInstantConverter implements Converter<Date, Instant> {
        INSTANCE;

        @Override
        public Instant convert(Date source) {
            return source.toInstant();
        }
    }

    @WritingConverter
    public enum InstantToDateConverter implements Converter<Instant, Date> {
        INSTANCE;

        @Override
        public Date convert(Instant source) {
            return Date.from(source);
        }
    }

    @ReadingConverter
    public enum LocalDateTimeToInstantConverter implements Converter<LocalDateTime, Instant> {
        INSTANCE;

        @Override
        public Instant convert(LocalDateTime source) {
            return source.atZone(systemDefault()).toInstant();
        }
    }

    @ReadingConverter
    public enum InstantToLocalDateTimeConverter implements Converter<Instant, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(Instant source) {
            return LocalDateTime.ofInstant(source, systemDefault());
        }
    }

    @WritingConverter
    public enum ZoneIdToStringConverter implements Converter<ZoneId, String> {
        INSTANCE;

        @Override
        public String convert(ZoneId source) {
            return source.toString();
        }
    }

    @ReadingConverter
    public enum StringToZoneIdConverter implements Converter<String, ZoneId> {
        INSTANCE;

        @Override
        public ZoneId convert(String source) {
            return ZoneId.of(source);
        }
    }

    @WritingConverter
    public enum DurationToStringConverter implements Converter<Duration, String> {
        INSTANCE;

        @Override
        public String convert(Duration duration) {
            return duration.toString();
        }
    }

    @ReadingConverter
    public enum StringToDurationConverter implements Converter<String, Duration> {
        INSTANCE;

        @Override
        public Duration convert(String s) {
            return Duration.parse(s);
        }
    }

    @WritingConverter
    public enum PeriodToStringConverter implements Converter<Period, String> {
        INSTANCE;

        @Override
        public String convert(Period period) {
            return period.toString();
        }
    }

    @ReadingConverter
    public enum StringToPeriodConverter implements Converter<String, Period> {
        INSTANCE;

        @Override
        public Period convert(String s) {
            return Period.parse(s);
        }
    }

    @ReadingConverter
    public enum StringToLocalDateConverter implements Converter<String, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DateTimeFormatter.ISO_DATE);
        }
    }

    @ReadingConverter
    public enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(String source) {
            return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    @ReadingConverter
    public enum StringToInstantConverter implements Converter<String, Instant> {
        INSTANCE;

        @Override
        public Instant convert(String source) {
            return Instant.parse(source);
        }
    }
}
