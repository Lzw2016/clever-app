package org.clever.data.redis.core.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.data.convert.ReadingConverter;
import org.clever.data.convert.WritingConverter;
import org.clever.data.redis.core.convert.BinaryConverters.StringBasedConverter;
import org.clever.util.ClassUtils;

import java.time.*;
import java.util.*;

/**
 * 帮助类注册 JSR-310 特定的 {@link Converter} 实现，以防我们在 Java 8 上运行
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:05 <br/>
 */
public abstract class Jsr310Converters {
    private static final boolean JAVA_8_IS_PRESENT = ClassUtils.isPresent("java.time.LocalDateTime", Jsr310Converters.class.getClassLoader());

    /**
     * 返回要注册的转换器。如果我们在 Java 8 上运行，将只返回转换器
     */
    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        if (!JAVA_8_IS_PRESENT) {
            return Collections.emptySet();
        }
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateTimeToBytesConverter());
        converters.add(new BytesToLocalDateTimeConverter());
        converters.add(new LocalDateToBytesConverter());
        converters.add(new BytesToLocalDateConverter());
        converters.add(new LocalTimeToBytesConverter());
        converters.add(new BytesToLocalTimeConverter());
        converters.add(new ZonedDateTimeToBytesConverter());
        converters.add(new BytesToZonedDateTimeConverter());
        converters.add(new InstantToBytesConverter());
        converters.add(new BytesToInstantConverter());
        converters.add(new ZoneIdToBytesConverter());
        converters.add(new BytesToZoneIdConverter());
        converters.add(new PeriodToBytesConverter());
        converters.add(new BytesToPeriodConverter());
        converters.add(new DurationToBytesConverter());
        converters.add(new BytesToDurationConverter());
        return converters;
    }

    public static boolean supports(Class<?> type) {
        if (!JAVA_8_IS_PRESENT) {
            return false;
        }
        return Arrays.<Class<?>>asList(
                LocalDateTime.class,
                LocalDate.class,
                LocalTime.class,
                Instant.class,
                ZonedDateTime.class,
                ZoneId.class,
                Period.class,
                Duration.class
        ).contains(type);
    }

    @WritingConverter
    static class LocalDateTimeToBytesConverter extends StringBasedConverter implements Converter<LocalDateTime, byte[]> {
        @Override
        public byte[] convert(LocalDateTime source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToLocalDateTimeConverter extends StringBasedConverter implements Converter<byte[], LocalDateTime> {
        @Override
        public LocalDateTime convert(byte[] source) {
            return LocalDateTime.parse(toString(source));
        }
    }

    @WritingConverter
    static class LocalDateToBytesConverter extends StringBasedConverter implements Converter<LocalDate, byte[]> {
        @Override
        public byte[] convert(LocalDate source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToLocalDateConverter extends StringBasedConverter implements Converter<byte[], LocalDate> {
        @Override
        public LocalDate convert(byte[] source) {
            return LocalDate.parse(toString(source));
        }
    }

    @WritingConverter
    static class LocalTimeToBytesConverter extends StringBasedConverter implements Converter<LocalTime, byte[]> {
        @Override
        public byte[] convert(LocalTime source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToLocalTimeConverter extends StringBasedConverter implements Converter<byte[], LocalTime> {
        @Override
        public LocalTime convert(byte[] source) {
            return LocalTime.parse(toString(source));
        }
    }

    @WritingConverter
    static class ZonedDateTimeToBytesConverter extends StringBasedConverter implements Converter<ZonedDateTime, byte[]> {
        @Override
        public byte[] convert(ZonedDateTime source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToZonedDateTimeConverter extends StringBasedConverter implements Converter<byte[], ZonedDateTime> {
        @Override
        public ZonedDateTime convert(byte[] source) {
            return ZonedDateTime.parse(toString(source));
        }
    }

    @WritingConverter
    static class InstantToBytesConverter extends StringBasedConverter implements Converter<Instant, byte[]> {
        @Override
        public byte[] convert(Instant source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToInstantConverter extends StringBasedConverter implements Converter<byte[], Instant> {
        @Override
        public Instant convert(byte[] source) {
            return Instant.parse(toString(source));
        }
    }

    @WritingConverter
    static class ZoneIdToBytesConverter extends StringBasedConverter implements Converter<ZoneId, byte[]> {
        @Override
        public byte[] convert(ZoneId source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToZoneIdConverter extends StringBasedConverter implements Converter<byte[], ZoneId> {
        @Override
        public ZoneId convert(byte[] source) {
            return ZoneId.of(toString(source));
        }
    }

    @WritingConverter
    static class PeriodToBytesConverter extends StringBasedConverter implements Converter<Period, byte[]> {
        @Override
        public byte[] convert(Period source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToPeriodConverter extends StringBasedConverter implements Converter<byte[], Period> {
        @Override
        public Period convert(byte[] source) {
            return Period.parse(toString(source));
        }
    }

    @WritingConverter
    static class DurationToBytesConverter extends StringBasedConverter implements Converter<Duration, byte[]> {
        @Override
        public byte[] convert(Duration source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToDurationConverter extends StringBasedConverter implements Converter<byte[], Duration> {
        @Override
        public Duration convert(byte[] source) {
            return Duration.parse(toString(source));
        }
    }
}
