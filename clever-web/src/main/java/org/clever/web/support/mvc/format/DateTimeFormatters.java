package org.clever.web.support.mvc.format;

import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * {@link DateTimeFormatter Formatters} 用于日期、时间和日期时间。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/10 15:14 <br/>
 */
public class DateTimeFormatters {
    private DateTimeFormatter dateFormatter;
    private String datePattern;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateTimeFormatter;

    /**
     * 使用给定的 {@code pattern} 配置日期格式
     *
     * @param pattern 格式化日期的模式
     * @return {@code this} 用于链式方法调用
     */
    public DateTimeFormatters dateFormat(String pattern) {
        if (isIso(pattern)) {
            this.dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            this.datePattern = "yyyy-MM-dd";
        } else {
            this.dateFormatter = formatter(pattern);
            this.datePattern = pattern;
        }
        return this;
    }

    /**
     * 使用给定的 {@code pattern} 配置时间格式
     *
     * @param pattern 格式化时间的模式
     * @return {@code this} 用于链式方法调用
     */
    public DateTimeFormatters timeFormat(String pattern) {
        this.timeFormatter = isIso(pattern) ? DateTimeFormatter.ISO_LOCAL_TIME : (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_TIME : formatter(pattern));
        return this;
    }

    /**
     * 使用给定的 {@code pattern} 配置日期时间格式
     *
     * @param pattern 格式化日期时间的模式
     * @return {@code this} 用于链式方法调用
     */
    public DateTimeFormatters dateTimeFormat(String pattern) {
        this.dateTimeFormatter = isIso(pattern) ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME : formatter(pattern));
        return this;
    }

    DateTimeFormatter getDateFormatter() {
        return this.dateFormatter;
    }

    String getDatePattern() {
        return this.datePattern;
    }

    DateTimeFormatter getTimeFormatter() {
        return this.timeFormatter;
    }

    DateTimeFormatter getDateTimeFormatter() {
        return this.dateTimeFormatter;
    }

    boolean isCustomized() {
        return this.dateFormatter != null || this.timeFormatter != null || this.dateTimeFormatter != null;
    }

    private static DateTimeFormatter formatter(String pattern) {
        return StringUtils.hasText(pattern) ? DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART) : null;
    }

    private static boolean isIso(String pattern) {
        return "iso".equalsIgnoreCase(pattern);
    }

    private static boolean isIsoOffset(String pattern) {
        return "isooffset".equalsIgnoreCase(pattern) || "iso-offset".equalsIgnoreCase(pattern);
    }
}
