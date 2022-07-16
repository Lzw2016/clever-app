package org.clever.format.support;

import org.clever.core.convert.support.DefaultConversionService;
import org.clever.format.FormatterRegistry;
import org.clever.format.datetime.DateFormatterRegistrar;
import org.clever.format.datetime.standard.DateTimeFormatterRegistrar;
import org.clever.format.number.NumberFormatAnnotationFormatterFactory;
import org.clever.format.number.money.CurrencyUnitFormatter;
import org.clever.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.clever.format.number.money.MonetaryAmountFormatter;
import org.clever.util.ClassUtils;
import org.clever.util.StringValueResolver;

/**
 * 同时支持类型转换和格式化，内置了常用的转换器和格式化器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:06 <br/>
 */
public class DefaultFormattingConversionService extends FormattingConversionService {
    /**
     * 当前运行时是否支持jsr354
     */
    private static final boolean jsr354Present;
    /**
     * 当前运行时是否支持jodaTime
     */
    private static final boolean jodaTimePresent;

    static {
        ClassLoader classLoader = DefaultFormattingConversionService.class.getClassLoader();
        jsr354Present = ClassUtils.isPresent("javax.money.MonetaryAmount", classLoader);
        jodaTimePresent = ClassUtils.isPresent("org.joda.time.YearMonth", classLoader);
    }

    /**
     * 创建一个新的 {@code DefaultFormattingConversionService}，注册默认的转换器和格式化器
     */
    public DefaultFormattingConversionService() {
        this(true, null);
    }

    /**
     * 创建一个新的 {@code DefaultFormattingConversionService}，注册默认的转换器和格式化器
     */
    public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
        this(registerDefaultFormatters, null);
    }

    /**
     * 创建一个新的 {@code DefaultFormattingConversionService}，注册默认的转换器，并自定是否注册默认的格式化器
     */
    public DefaultFormattingConversionService(boolean registerDefaultFormatters, StringValueResolver stringValueResolver) {
        DefaultConversionService.addDefaultConverters(this);
        if (registerDefaultFormatters) {
            addDefaultFormatters(this, stringValueResolver);
        }
    }

    /**
     * 为{@code FormatterRegistry}注册默认的格式化器
     */
    public static void addDefaultFormatters(FormatterRegistry formatterRegistry, StringValueResolver stringValueResolver) {
        // Default handling of number values
        formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory(stringValueResolver));

        // Default handling of monetary values
        if (jsr354Present) {
            formatterRegistry.addFormatter(new CurrencyUnitFormatter());
            formatterRegistry.addFormatter(new MonetaryAmountFormatter());
            formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory(stringValueResolver));
        }

        // Default handling of date-time values

        // just handling JSR-310 specific date and time types
        new DateTimeFormatterRegistrar(stringValueResolver).registerFormatters(formatterRegistry);

        if (jodaTimePresent) {
            // handles Joda-specific types as well as Date, Calendar, Long
            new org.clever.format.datetime.joda.JodaTimeFormatterRegistrar(stringValueResolver).registerFormatters(formatterRegistry);
        } else {
            // regular DateFormat-based Date, Calendar, Long converters
            new DateFormatterRegistrar(stringValueResolver).registerFormatters(formatterRegistry);
        }
    }
}