package org.clever.format.number.money;

import org.clever.format.AnnotationFormatterFactory;
import org.clever.format.Formatter;
import org.clever.format.Parser;
import org.clever.format.Printer;
import org.clever.format.annotation.NumberFormat;
import org.clever.format.annotation.NumberFormat.Style;
import org.clever.format.number.CurrencyStyleFormatter;
import org.clever.format.number.NumberStyleFormatter;
import org.clever.format.number.PercentStyleFormatter;
import org.clever.util.StringUtils;
import org.clever.util.StringValueResolver;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.text.ParseException;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * 格式化标注了{@link NumberFormat}注解的{@link javax.money.MonetaryAmount}字段
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 19:55 <br/>
 */
public class Jsr354NumberFormatAnnotationFormatterFactory implements AnnotationFormatterFactory<NumberFormat> {
    private static final String CURRENCY_CODE_PATTERN = "\u00A4\u00A4";

    private final StringValueResolver stringValueResolver;

    public Jsr354NumberFormatAnnotationFormatterFactory(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    @Override
    public Set<Class<?>> getFieldTypes() {
        return Collections.singleton(MonetaryAmount.class);
    }

    @Override
    public Printer<MonetaryAmount> getPrinter(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    @Override
    public Parser<MonetaryAmount> getParser(NumberFormat annotation, Class<?> fieldType) {
        return configureFormatterFrom(annotation);
    }

    private Formatter<MonetaryAmount> configureFormatterFrom(NumberFormat annotation) {
        String pattern = annotation.pattern();
        if (stringValueResolver != null) {
            pattern = stringValueResolver.resolveStringValue(annotation.pattern());
        }
        if (StringUtils.hasLength(pattern)) {
            return new PatternDecoratingFormatter(pattern);
        } else {
            Style style = annotation.style();
            if (style == Style.NUMBER) {
                return new NumberDecoratingFormatter(new NumberStyleFormatter());
            } else if (style == Style.PERCENT) {
                return new NumberDecoratingFormatter(new PercentStyleFormatter());
            } else {
                return new NumberDecoratingFormatter(new CurrencyStyleFormatter());
            }
        }
    }

    private static class NumberDecoratingFormatter implements Formatter<MonetaryAmount> {
        private final Formatter<Number> numberFormatter;

        public NumberDecoratingFormatter(Formatter<Number> numberFormatter) {
            this.numberFormatter = numberFormatter;
        }

        @Override
        public String print(MonetaryAmount object, Locale locale) {
            return this.numberFormatter.print(object.getNumber(), locale);
        }

        @Override
        public MonetaryAmount parse(String text, Locale locale) throws ParseException {
            CurrencyUnit currencyUnit = Monetary.getCurrency(locale);
            Number numberValue = this.numberFormatter.parse(text, locale);
            return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
        }
    }

    private static class PatternDecoratingFormatter implements Formatter<MonetaryAmount> {
        private final String pattern;

        public PatternDecoratingFormatter(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String print(MonetaryAmount object, Locale locale) {
            CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
            formatter.setCurrency(Currency.getInstance(object.getCurrency().getCurrencyCode()));
            formatter.setPattern(this.pattern);
            return formatter.print(object.getNumber(), locale);
        }

        @Override
        public MonetaryAmount parse(String text, Locale locale) throws ParseException {
            CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
            Currency currency = determineCurrency(text, locale);
            CurrencyUnit currencyUnit = Monetary.getCurrency(currency.getCurrencyCode());
            formatter.setCurrency(currency);
            formatter.setPattern(this.pattern);
            Number numberValue = formatter.parse(text, locale);
            return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
        }

        private Currency determineCurrency(String text, Locale locale) {
            try {
                if (text.length() < 3) {
                    // Could not possibly contain a currency code ->
                    // try with locale and likely let it fail on parse.
                    return Currency.getInstance(locale);
                } else if (this.pattern.startsWith(CURRENCY_CODE_PATTERN)) {
                    return Currency.getInstance(text.substring(0, 3));
                } else if (this.pattern.endsWith(CURRENCY_CODE_PATTERN)) {
                    return Currency.getInstance(text.substring(text.length() - 3));
                } else {
                    // A pattern without a currency code...
                    return Currency.getInstance(locale);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Cannot determine currency for number value [" + text + "]", ex);
            }
        }
    }
}
