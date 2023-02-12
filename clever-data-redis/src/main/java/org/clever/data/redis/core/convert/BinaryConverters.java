package org.clever.data.redis.core.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.data.convert.ReadingConverter;
import org.clever.data.convert.WritingConverter;
import org.clever.util.NumberUtils;
import org.clever.util.ObjectUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * 用于将对象转换为二进制格式的 {@link ReadingConverter} 和 {@link WritingConverter} 集
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 11:02 <br/>
 */
final class BinaryConverters {
    /**
     * 使用 {@literal UTF-8} 作为默认字符集
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private BinaryConverters() {
    }

    static class StringBasedConverter {
        byte[] fromString(String source) {
            return source.getBytes(CHARSET);
        }

        String toString(byte[] source) {
            return new String(source, CHARSET);
        }
    }

    @WritingConverter
    static class StringToBytesConverter extends StringBasedConverter implements Converter<String, byte[]> {
        @Override
        public byte[] convert(String source) {
            return fromString(source);
        }
    }

    @ReadingConverter
    static class BytesToStringConverter extends StringBasedConverter implements Converter<byte[], String> {
        @Override
        public String convert(byte[] source) {
            return toString(source);
        }
    }

    @WritingConverter
    static class NumberToBytesConverter extends StringBasedConverter implements Converter<Number, byte[]> {
        @Override
        public byte[] convert(Number source) {
            return fromString(source.toString());
        }
    }

    @WritingConverter
    static class EnumToBytesConverter extends StringBasedConverter implements Converter<Enum<?>, byte[]> {
        @Override
        public byte[] convert(Enum<?> source) {
            return fromString(source.name());
        }
    }

    @ReadingConverter
    static final class BytesToEnumConverterFactory implements ConverterFactory<byte[], Enum<?>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <T extends Enum<?>> Converter<byte[], T> getConverter(Class<T> targetType) {
            Class<?> enumType = targetType;
            while (enumType != null && !enumType.isEnum()) {
                enumType = enumType.getSuperclass();
            }
            if (enumType == null) {
                // noinspection ConstantConditions
                throw new IllegalArgumentException("The target type " + targetType.getName() + " does not refer to an enum");
            }
            return new BytesToEnum(enumType);
        }

        private static class BytesToEnum<T extends Enum<T>> extends StringBasedConverter implements Converter<byte[], T> {
            private final Class<T> enumType;

            public BytesToEnum(Class<T> enumType) {
                this.enumType = enumType;
            }

            @Override
            public T convert(byte[] source) {
                if (ObjectUtils.isEmpty(source)) {
                    return null;
                }
                return Enum.valueOf(this.enumType, toString(source).trim());
            }
        }
    }

    @ReadingConverter
    static class BytesToNumberConverterFactory implements ConverterFactory<byte[], Number> {
        @Override
        public <T extends Number> Converter<byte[], T> getConverter(Class<T> targetType) {
            return new BytesToNumberConverter<>(targetType);
        }

        private static final class BytesToNumberConverter<T extends Number> extends StringBasedConverter implements Converter<byte[], T> {
            private final Class<T> targetType;

            public BytesToNumberConverter(Class<T> targetType) {
                this.targetType = targetType;
            }

            @Override
            public T convert(byte[] source) {
                if (ObjectUtils.isEmpty(source)) {
                    return null;
                }
                return NumberUtils.parseNumber(toString(source), targetType);
            }
        }
    }

    @WritingConverter
    static class BooleanToBytesConverter extends StringBasedConverter implements Converter<Boolean, byte[]> {
        byte[] _true = fromString("1");
        byte[] _false = fromString("0");

        @Override
        public byte[] convert(Boolean source) {
            return source ? _true : _false;
        }
    }

    @ReadingConverter
    static class BytesToBooleanConverter extends StringBasedConverter implements Converter<byte[], Boolean> {
        @Override
        public Boolean convert(byte[] source) {
            if (ObjectUtils.isEmpty(source)) {
                return null;
            }
            String value = toString(source);
            return ("1".equals(value) || "true".equalsIgnoreCase(value)) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @WritingConverter
    static class DateToBytesConverter extends StringBasedConverter implements Converter<Date, byte[]> {
        @Override
        public byte[] convert(Date source) {
            return fromString(Long.toString(source.getTime()));
        }
    }

    @ReadingConverter
    static class BytesToDateConverter extends StringBasedConverter implements Converter<byte[], Date> {
        @Override
        public Date convert(byte[] source) {
            if (ObjectUtils.isEmpty(source)) {
                return null;
            }
            String value = toString(source);
            try {
                return new Date(NumberUtils.parseNumber(value, Long.class));
            } catch (NumberFormatException nfe) {
                // ignore
            }
            try {
                return DateFormat.getInstance().parse(value);
            } catch (ParseException e) {
                // ignore
            }
            throw new IllegalArgumentException(String.format("Cannot parse date out of %s", Arrays.toString(source)));
        }
    }

    @WritingConverter
    static class UuidToBytesConverter extends StringBasedConverter implements Converter<UUID, byte[]> {
        @Override
        public byte[] convert(UUID source) {
            return fromString(source.toString());
        }
    }

    @ReadingConverter
    static class BytesToUuidConverter extends StringBasedConverter implements Converter<byte[], UUID> {
        @Override
        public UUID convert(byte[] source) {
            if (ObjectUtils.isEmpty(source)) {
                return null;
            }
            return UUID.fromString(toString(source));
        }
    }
}
