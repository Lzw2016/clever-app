package org.clever.format.datetime.standard;

import org.clever.format.Parser;
import org.clever.util.ObjectUtils;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * JSR-310 {@link java.time.temporal.TemporalAccessor}的解析器实现，使用{@link java.time.format.DateTimeFormatter}(上下文中如果有一个)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:22 <br/>
 */
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {
    private final Class<? extends TemporalAccessor> temporalAccessorType;
    private final DateTimeFormatter formatter;
    private final String[] fallbackPatterns;
    private final Object source;

    /**
     * 为给定的TemporalAccessor类型创建一个新的TemporalAccessorParser
     *
     * @param temporalAccessorType 特定的TemporalAccessor类 (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime, OffsetTime)
     * @param formatter            基础DateTimeFormatter实例
     */
    public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
        this(temporalAccessorType, formatter, null, null);
    }

    TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter, String[] fallbackPatterns, Object source) {
        this.temporalAccessorType = temporalAccessorType;
        this.formatter = formatter;
        this.fallbackPatterns = fallbackPatterns;
        this.source = source;
    }

    @Override
    public TemporalAccessor parse(String text, Locale locale) throws ParseException {
        try {
            return doParse(text, locale, this.formatter);
        } catch (DateTimeParseException ex) {
            if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
                for (String pattern : this.fallbackPatterns) {
                    try {
                        DateTimeFormatter fallbackFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(pattern);
                        return doParse(text, locale, fallbackFormatter);
                    } catch (DateTimeParseException ignoredException) {
                        // Ignore fallback parsing exceptions since the exception thrown below
                        // will include information from the "source" if available -- for example,
                        // the toString() of a @DateTimeFormat annotation.
                    }
                }
            }
            if (this.source != null) {
                throw new DateTimeParseException(
                        String.format(
                                "Unable to parse date time value \"%s\" using configuration from %s",
                                text,
                                this.source
                        ),
                        text,
                        ex.getErrorIndex(),
                        ex
                );
            }
            // else rethrow original exception
            throw ex;
        }
    }

    private TemporalAccessor doParse(String text, Locale locale, DateTimeFormatter formatter) throws DateTimeParseException {
        DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(formatter, locale);
        if (LocalDate.class == this.temporalAccessorType) {
            return LocalDate.parse(text, formatterToUse);
        } else if (LocalTime.class == this.temporalAccessorType) {
            return LocalTime.parse(text, formatterToUse);
        } else if (LocalDateTime.class == this.temporalAccessorType) {
            return LocalDateTime.parse(text, formatterToUse);
        } else if (ZonedDateTime.class == this.temporalAccessorType) {
            return ZonedDateTime.parse(text, formatterToUse);
        } else if (OffsetDateTime.class == this.temporalAccessorType) {
            return OffsetDateTime.parse(text, formatterToUse);
        } else if (OffsetTime.class == this.temporalAccessorType) {
            return OffsetTime.parse(text, formatterToUse);
        } else {
            throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
        }
    }
}
