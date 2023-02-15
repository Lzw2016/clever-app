package org.clever.data.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.util.ClassUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * Helper 类用于注册 JodaTime 特定的 {@link Converter} 实现，以防库存在于类路径中。<br />
 * 建议：使用 JSR-310 类型替代 Joda-Time
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:09 <br/>
 */
public abstract class JodaTimeConverters {
    private static final boolean JODA_TIME_IS_PRESENT = ClassUtils.isPresent("org.joda.time.LocalDate", null);

    /**
     * 返回要注册的转换器。只有在类中存在 JodaTime 的情况下才会返回转换器。
     */
    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        if (!JODA_TIME_IS_PRESENT) {
            return Collections.emptySet();
        }
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(LocalDateToDateConverter.INSTANCE);
        converters.add(LocalDateTimeToDateConverter.INSTANCE);
        converters.add(DateTimeToDateConverter.INSTANCE);
        converters.add(DateToLocalDateConverter.INSTANCE);
        converters.add(DateToLocalDateTimeConverter.INSTANCE);
        converters.add(DateToDateTimeConverter.INSTANCE);
        converters.add(LocalDateTimeToJodaLocalDateTime.INSTANCE);
        converters.add(LocalDateTimeToJodaDateTime.INSTANCE);
        converters.add(InstantToJodaLocalDateTime.INSTANCE);
        converters.add(JodaLocalDateTimeToInstant.INSTANCE);
        converters.add(LocalDateTimeToJsr310Converter.INSTANCE);
        return converters;
    }

    public enum LocalDateTimeToJsr310Converter implements Converter<LocalDateTime, java.time.LocalDateTime> {
        INSTANCE;

        @Override
        public java.time.LocalDateTime convert(LocalDateTime source) {
            return java.time.LocalDateTime.ofInstant(source.toDate().toInstant(), ZoneId.systemDefault());
        }
    }

    public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalDate source) {
            return source.toDate();
        }
    }

    public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        INSTANCE;

        @Override
        public Date convert(LocalDateTime source) {
            return source.toDate();
        }
    }

    public enum DateTimeToDateConverter implements Converter<DateTime, Date> {
        INSTANCE;

        @Override
        public Date convert(DateTime source) {
            return source.toDate();
        }
    }

    public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(Date source) {
            return new LocalDate(source.getTime());
        }
    }

    public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(Date source) {
            return new LocalDateTime(source.getTime());
        }
    }

    public enum DateToDateTimeConverter implements Converter<Date, DateTime> {
        INSTANCE;

        @Override
        public DateTime convert(Date source) {
            return new DateTime(source.getTime());
        }
    }

    @ReadingConverter
    public enum LocalDateTimeToJodaLocalDateTime implements Converter<java.time.LocalDateTime, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(java.time.LocalDateTime source) {
            return LocalDateTime.fromDateFields(Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
        }
    }

    public enum InstantToJodaLocalDateTime implements Converter<java.time.Instant, LocalDateTime> {
        INSTANCE;

        @Override
        public LocalDateTime convert(java.time.Instant source) {
            return LocalDateTime.fromDateFields(new Date(source.toEpochMilli()));
        }
    }

    public enum JodaLocalDateTimeToInstant implements Converter<LocalDateTime, Instant> {
        INSTANCE;

        @Override
        public Instant convert(LocalDateTime source) {
            return Instant.ofEpochMilli(source.toDateTime().getMillis());
        }
    }

    public enum LocalDateTimeToJodaDateTime implements Converter<java.time.LocalDateTime, DateTime> {
        INSTANCE;

        @Override
        public DateTime convert(java.time.LocalDateTime source) {
            return new DateTime(Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
        }
    }
}
