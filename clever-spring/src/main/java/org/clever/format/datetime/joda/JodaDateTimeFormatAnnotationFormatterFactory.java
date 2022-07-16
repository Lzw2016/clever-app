package org.clever.format.datetime.joda;

import org.clever.format.AnnotationFormatterFactory;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.DateTimeFormat;
import org.clever.util.StringUtils;
import org.clever.util.StringValueResolver;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

/**
 * 使用Joda Time格式化被{@link DateTimeFormat}注解批注的字段
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 21:00 <br/>
 */
public class JodaDateTimeFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<DateTimeFormat> {
    private static final Set<Class<?>> FIELD_TYPES;

    static {
        // Create the set of field types that may be annotated with @DateTimeFormat.
        // Note: the 3 ReadablePartial concrete types are registered explicitly since
        // addFormatterForFieldType rules exist for each of these types
        // (if we did not do this, the default byType rules for LocalDate, LocalTime,
        // and LocalDateTime would take precedence over the annotation rule, which
        // is not what we want)
        Set<Class<?>> fieldTypes = new HashSet<>(8);
        fieldTypes.add(ReadableInstant.class);
        fieldTypes.add(LocalDate.class);
        fieldTypes.add(LocalTime.class);
        fieldTypes.add(LocalDateTime.class);
        fieldTypes.add(Date.class);
        fieldTypes.add(Calendar.class);
        fieldTypes.add(Long.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    private final StringValueResolver stringValueResolver;

    public JodaDateTimeFormatAnnotationFormatterFactory(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    @Override
    public final Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatter formatter = getFormatter(annotation, fieldType);
        if (ReadablePartial.class.isAssignableFrom(fieldType)) {
            return new ReadablePartialPrinter(formatter);
        } else if (ReadableInstant.class.isAssignableFrom(fieldType) || Calendar.class.isAssignableFrom(fieldType)) {
            // assumes Calendar->ReadableInstant converter is registered
            return new ReadableInstantPrinter(formatter);
        } else {
            // assumes Date->Long converter is registered
            return new MillisecondInstantPrinter(formatter);
        }
    }

    @Override
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        if (LocalDate.class == fieldType) {
            return new LocalDateParser(getFormatter(annotation, fieldType));
        } else if (LocalTime.class == fieldType) {
            return new LocalTimeParser(getFormatter(annotation, fieldType));
        } else if (LocalDateTime.class == fieldType) {
            return new LocalDateTimeParser(getFormatter(annotation, fieldType));
        } else {
            return new DateTimeParser(getFormatter(annotation, fieldType));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
        String style = annotation.style();
        if (stringValueResolver != null) {
            style = stringValueResolver.resolveStringValue(annotation.style());
        }
        if (StringUtils.hasLength(style)) {
            factory.setStyle(style);
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
}
