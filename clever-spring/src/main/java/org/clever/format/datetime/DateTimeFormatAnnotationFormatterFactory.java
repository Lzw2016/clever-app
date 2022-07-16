package org.clever.format.datetime;

import org.clever.format.AnnotationFormatterFactory;
import org.clever.format.Formatter;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.DateTimeFormat;
import org.clever.util.StringUtils;
import org.clever.util.StringValueResolver;

import java.util.*;

/**
 * 使用{@link DateTimeFormat}格式化被{@link DateFormatter}注解批注的字段
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:11 <br/>
 */
public class DateTimeFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<DateTimeFormat> {
    private static final Set<Class<?>> FIELD_TYPES;

    private final StringValueResolver stringValueResolver;

    public DateTimeFormatAnnotationFormatterFactory(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    static {
        Set<Class<?>> fieldTypes = new HashSet<>(4);
        fieldTypes.add(Date.class);
        fieldTypes.add(Calendar.class);
        fieldTypes.add(Long.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    @Override
    public Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        return getFormatter(annotation, fieldType);
    }

    @Override
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        return getFormatter(annotation, fieldType);
    }

    @SuppressWarnings("DuplicatedCode")
    protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateFormatter formatter = new DateFormatter();
        formatter.setSource(annotation);
        formatter.setIso(annotation.iso());

        String style = annotation.style();
        if (stringValueResolver != null) {
            style = stringValueResolver.resolveStringValue(annotation.style());
        }
        if (StringUtils.hasLength(style)) {
            formatter.setStylePattern(style);
        }

        String pattern = annotation.pattern();
        if (stringValueResolver != null) {
            pattern = stringValueResolver.resolveStringValue(annotation.pattern());
        }
        if (StringUtils.hasLength(pattern)) {
            formatter.setPattern(pattern);
        }

        List<String> resolvedFallbackPatterns = new ArrayList<>();
        for (String fallbackPattern : annotation.fallbackPatterns()) {
            if (stringValueResolver != null) {
                fallbackPattern = stringValueResolver.resolveStringValue(fallbackPattern);
            }
            if (StringUtils.hasLength(fallbackPattern)) {
                resolvedFallbackPatterns.add(fallbackPattern);
            }
        }
        if (!resolvedFallbackPatterns.isEmpty()) {
            formatter.setFallbackPatterns(resolvedFallbackPatterns.toArray(new String[0]));
        }

        return formatter;
    }
}
