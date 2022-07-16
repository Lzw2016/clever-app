package org.clever.format.datetime.joda;

import org.clever.format.annotation.DateTimeFormat.ISO;
import org.clever.util.StringUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.TimeZone;

/**
 * Factory that creates a Joda-Time {@link DateTimeFormatter}.
 *
 * <p>Formatters will be created using the defined {@link #setPattern pattern},
 * {@link #setIso ISO}, and {@link #setStyle style} methods (considered in that order).
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:42 <br/>
 */
public class DateTimeFormatterFactory {
    /**
     * 自定义格式
     */
    private String pattern;
    private ISO iso;
    private String style;
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

    /**
     * @param style two characters from the set {"S", "M", "L", "F", "-"}
     */
    public void setStyle(String style) {
        this.style = style;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public DateTimeFormatter createDateTimeFormatter() {
        return createDateTimeFormatter(DateTimeFormat.mediumDateTime());
    }

    public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
        DateTimeFormatter dateTimeFormatter = null;
        if (StringUtils.hasLength(this.pattern)) {
            dateTimeFormatter = DateTimeFormat.forPattern(this.pattern);
        } else if (this.iso != null && this.iso != ISO.NONE) {
            switch (this.iso) {
                case DATE:
                    dateTimeFormatter = ISODateTimeFormat.date();
                    break;
                case TIME:
                    dateTimeFormatter = ISODateTimeFormat.time();
                    break;
                case DATE_TIME:
                    dateTimeFormatter = ISODateTimeFormat.dateTime();
                    break;
                default:
                    throw new IllegalStateException("Unsupported ISO format: " + this.iso);
            }
        } else if (StringUtils.hasLength(this.style)) {
            dateTimeFormatter = DateTimeFormat.forStyle(this.style);
        }

        if (dateTimeFormatter != null && this.timeZone != null) {
            dateTimeFormatter = dateTimeFormatter.withZone(DateTimeZone.forTimeZone(this.timeZone));
        }
        return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
    }
}
