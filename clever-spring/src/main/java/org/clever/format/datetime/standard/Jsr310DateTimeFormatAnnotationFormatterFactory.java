package org.clever.format.datetime.standard;

import org.clever.format.AnnotationFormatterFactory;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.DateTimeFormat;
import org.clever.util.StringUtils;
import org.clever.util.StringValueResolver;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * 使用JSR-310 {@code java.time}格式化被{@link DateTimeFormat}注解批注的字段
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:34 <br/>
 */
public class Jsr310DateTimeFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<DateTimeFormat> {
    private static final Set<Class<?>> FIELD_TYPES;

    static {
        // Create the set of field types that may be annotated with @DateTimeFormat.
        Set<Class<?>> fieldTypes = new HashSet<>(8);
        fieldTypes.add(LocalDate.class);
        fieldTypes.add(LocalTime.class);
        fieldTypes.add(LocalDateTime.class);
        fieldTypes.add(ZonedDateTime.class);
        fieldTypes.add(OffsetDateTime.class);
        fieldTypes.add(OffsetTime.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    private final StringValueResolver stringValueResolver;

    public Jsr310DateTimeFormatAnnotationFormatterFactory(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    @Override
    public final Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatter formatter = getFormatter(annotation, fieldType);

        // Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
        if (formatter == DateTimeFormatter.ISO_DATE) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            }
        } else if (formatter == DateTimeFormatter.ISO_TIME) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_TIME;
            }
        } else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            }
        }

        return new TemporalAccessorPrinter(formatter);
    }

    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    @Override
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        List<String> resolvedFallbackPatterns = new ArrayList<>();
        for (String fallbackPattern : annotation.fallbackPatterns()) {
            if (stringValueResolver != null) {
                fallbackPattern = stringValueResolver.resolveStringValue(fallbackPattern);
            }
            if (StringUtils.hasLength(fallbackPattern)) {
                resolvedFallbackPatterns.add(fallbackPattern);
            }
        }

        DateTimeFormatter formatter = getFormatter(annotation, fieldType);
        return new TemporalAccessorParser(
                (Class<? extends TemporalAccessor>) fieldType,
                formatter,
                resolvedFallbackPatterns.toArray(new String[0]),
                annotation
        );
    }

    @SuppressWarnings("DuplicatedCode")
    protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
        String style = annotation.style();
        if (stringValueResolver != null) {
            style = stringValueResolver.resolveStringValue(annotation.style());
        }
        if (StringUtils.hasLength(style)) {
            factory.setStylePattern(style);
        }
        factory.setIso(annotation.iso());
        String pattern = annotation.pattern();
        if (stringValueResolver != null) {
            pattern = stringValueResolver.resolveStringValue(annotation.pattern());
        }
        if (StringUtils.hasLength(pattern)) {
            factory.setPattern(pattern);
        }
        return factory.createDateTimeFormatter();
    }

    private boolean isLocal(Class<?> fieldType) {
        return fieldType.getSimpleName().startsWith("Local");
    }
}
