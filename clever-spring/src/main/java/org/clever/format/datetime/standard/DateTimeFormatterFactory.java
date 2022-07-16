package org.clever.format.datetime.standard;

import org.clever.format.annotation.DateTimeFormat.ISO;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.TimeZone;

/**
 * 创建JSR-310 {@link java.time.format.DateTimeFormatter}的工厂。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:01 <br/>
 */
public class DateTimeFormatterFactory {
    /**
     * 自定义格式
     */
    private String pattern;
    private ISO iso;
    private FormatStyle dateStyle;
    private FormatStyle timeStyle;
    private TimeZone timeZone;

    public DateTimeFormatterFactory() {
    }

    public DateTimeFormatterFactory(String pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setIso(ISO iso) {
        this.iso = iso;
    }

    public void setDateStyle(FormatStyle dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setTimeStyle(FormatStyle timeStyle) {
        this.timeStyle = timeStyle;
    }

    public void setDateTimeStyle(FormatStyle dateTimeStyle) {
        this.dateStyle = dateTimeStyle;
        this.timeStyle = dateTimeStyle;
    }

    /**
     * @param style two characters from the set {"S", "M", "L", "F", "-"}
     */
    public void setStylePattern(String style) {
        Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
        this.dateStyle = convertStyleCharacter(style.charAt(0));
        this.timeStyle = convertStyleCharacter(style.charAt(1));
    }

    private FormatStyle convertStyleCharacter(char c) {
        switch (c) {
            case 'S':
                return FormatStyle.SHORT;
            case 'M':
                return FormatStyle.MEDIUM;
            case 'L':
                return FormatStyle.LONG;
            case 'F':
                return FormatStyle.FULL;
            case '-':
                return null;
            default:
                throw new IllegalArgumentException("Invalid style character '" + c + "'");
        }
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * 使用此工厂创建新的DateTimeFormatter。
     * 如果没有定义特定的模式或样式，将使用{@link FormatStyle#MEDIUM}日期时间格式
     *
     * @see #createDateTimeFormatter(DateTimeFormatter)
     */
    public DateTimeFormatter createDateTimeFormatter() {
        return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    /**
     * 使用此工厂创建新的DateTimeFormatter。
     * 如果没有定义特定的模式或样式，将使用提供的{@code fallbackFormatter}
     */
    public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
        DateTimeFormatter dateTimeFormatter = null;
        if (StringUtils.hasLength(this.pattern)) {
            dateTimeFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(this.pattern);
        } else if (this.iso != null && this.iso != ISO.NONE) {
            switch (this.iso) {
                case DATE:
                    dateTimeFormatter = DateTimeFormatter.ISO_DATE;
                    break;
                case TIME:
                    dateTimeFormatter = DateTimeFormatter.ISO_TIME;
                    break;
                case DATE_TIME:
                    dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                    break;
                default:
                    throw new IllegalStateException("Unsupported ISO format: " + this.iso);
            }
        } else if (this.dateStyle != null && this.timeStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
        } else if (this.dateStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
        } else if (this.timeStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
        }

        if (dateTimeFormatter != null && this.timeZone != null) {
            dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
        }
        return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
    }
}
