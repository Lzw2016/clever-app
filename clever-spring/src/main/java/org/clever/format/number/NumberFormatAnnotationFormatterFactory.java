package org.clever.format.number;

import org.clever.format.AnnotationFormatterFactory;
import org.clever.format.Formatter;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.NumberFormat;
import org.clever.format.annotation.NumberFormat.Style;
import org.clever.util.NumberUtils;
import org.clever.util.StringUtils;
import org.clever.util.StringValueResolver;

import java.util.Set;

/**
 * 格式化使用{@link NumberFormat}注解的字段
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:12 <br/>
 */
public class NumberFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<NumberFormat> {
    private final StringValueResolver stringValueResolver;

    public NumberFormatAnnotationFormatterFactory(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    @Override
    public Set<Class<?>> getFieldTypes() {
        return NumberUtils.STANDARD_NUMBER_TYPES;
    }

    @Override
    public Printer<Number> getPrinter(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    @Override
    public Parser<Number> getParser(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    private Formatter<Number> configureFormatterFrom(NumberFormat annotation) {
        String pattern = annotation.pattern();
        if (stringValueResolver != null) {
            pattern = stringValueResolver.resolveStringValue(annotation.pattern());
        }
        if (StringUtils.hasLength(pattern)) {
            return new NumberStyleFormatter(pattern);
        } else {
            Style style = annotation.style();
            if (style == Style.CURRENCY) {
                return new CurrencyStyleFormatter();
            } else if (style == Style.PERCENT) {
                return new PercentStyleFormatter();
            } else {
                return new NumberStyleFormatter();
            }
        }
    }
}
