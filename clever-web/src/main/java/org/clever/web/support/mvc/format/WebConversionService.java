package org.clever.web.support.mvc.format;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.ClassUtils;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link FormattingConversionService} 专用于 Web 应用程序，用于从 Web 格式化和转换值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/10 15:00 <br/>
 */
public class WebConversionService extends DefaultFormattingConversionService {
    private static final boolean JSR_354_PRESENT = ClassUtils.isPresent("javax.money.MonetaryAmount", WebConversionService.class.getClassLoader());

    /**
     * 创建一个新的 WebConversionService，它使用提供的日期、时间和日期时间格式配置格式化程序，或者如果未提供自定义格式则注册默认格式。
     *
     * @param dateTimeFormatters 用于日期、时间和日期时间格式化的格式化程序
     */
    public WebConversionService(DateTimeFormatters dateTimeFormatters) {
        super(false);
        if (dateTimeFormatters.isCustomized()) {
            addFormatters(dateTimeFormatters);
        } else {
            addDefaultFormatters(this);
        }
    }

    private void addFormatters(DateTimeFormatters dateTimeFormatters) {
        addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
        if (JSR_354_PRESENT) {
            addFormatter(new CurrencyUnitFormatter());
            addFormatter(new MonetaryAmountFormatter());
            addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
        }
        registerJsr310(dateTimeFormatters);
        registerJavaDate(dateTimeFormatters);
    }

    private void registerJsr310(DateTimeFormatters dateTimeFormatters) {
        DateTimeFormatterRegistrar dateTime = new DateTimeFormatterRegistrar();
        configure(dateTimeFormatters::getDateFormatter, dateTime::setDateFormatter);
        configure(dateTimeFormatters::getTimeFormatter, dateTime::setTimeFormatter);
        configure(dateTimeFormatters::getDateTimeFormatter, dateTime::setDateTimeFormatter);
        dateTime.registerFormatters(this);
    }

    private void configure(Supplier<DateTimeFormatter> supplier, Consumer<DateTimeFormatter> consumer) {
        DateTimeFormatter formatter = supplier.get();
        if (formatter != null) {
            consumer.accept(formatter);
        }
    }

    private void registerJavaDate(DateTimeFormatters dateTimeFormatters) {
        DateFormatterRegistrar dateFormatterRegistrar = new DateFormatterRegistrar();
        String datePattern = dateTimeFormatters.getDatePattern();
        if (datePattern != null) {
            DateFormatter dateFormatter = new DateFormatter(datePattern);
            dateFormatter.setFallbackPatterns(
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
                "yyyyMMdd HH:mm:ss", "yyyyMMdd HH:mm", "yyyyMMdd",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            );
            dateFormatterRegistrar.setFormatter(dateFormatter);
        }
        dateFormatterRegistrar.registerFormatters(this);
    }
}
