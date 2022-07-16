package org.clever.format.datetime;

import org.clever.format.Formatter;
import org.clever.format.annotation.DateTimeFormat.ISO;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * {@link java.util.Date}类型的格式化程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:13 <br/>
 */
public class DateFormatter implements Formatter<Date> {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Map<ISO, String> ISO_PATTERNS;

    static {
        Map<ISO, String> formats = new EnumMap<>(ISO.class);
        formats.put(ISO.DATE, "yyyy-MM-dd");
        formats.put(ISO.TIME, "HH:mm:ss.SSSXXX");
        formats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        ISO_PATTERNS = Collections.unmodifiableMap(formats);
    }

    /**
     * 配置对象
     */
    private Object source;
    /**
     * 自定义格式
     */
    private String pattern;
    /**
     * 回退的时间格式
     */
    private String[] fallbackPatterns;
    private int style = DateFormat.DEFAULT;
    private String stylePattern;
    private ISO iso;
    private TimeZone timeZone;
    /**
     * 是否宽松模式
     */
    private boolean lenient = false;

    public DateFormatter() {
    }

    public DateFormatter(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 设置此DateFormatter的配置对象
     */
    public void setSource(Object source) {
        this.source = source;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setFallbackPatterns(String... fallbackPatterns) {
        this.fallbackPatterns = fallbackPatterns;
    }

    public void setIso(ISO iso) {
        this.iso = iso;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setStylePattern(String stylePattern) {
        this.stylePattern = stylePattern;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    @Override
    public String print(Date date, Locale locale) {
        return getDateFormat(locale).format(date);
    }

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        try {
            return getDateFormat(locale).parse(text);
        } catch (ParseException ex) {
            if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
                for (String pattern : this.fallbackPatterns) {
                    try {
                        DateFormat dateFormat = configureDateFormat(new SimpleDateFormat(pattern, locale));
                        // Align timezone for parsing format with printing format if ISO is set.
                        if (this.iso != null && this.iso != ISO.NONE) {
                            dateFormat.setTimeZone(UTC);
                        }
                        return dateFormat.parse(text);
                    } catch (ParseException ignoredException) {
                        // Ignore fallback parsing exceptions since the exception thrown below
                        // will include information from the "source" if available -- for example,
                        // the toString() of a @DateTimeFormat annotation.
                    }
                }
            }
            if (this.source != null) {
                ParseException parseException = new ParseException(
                        String.format(
                                "Unable to parse date time value \"%s\" using configuration from %s",
                                text,
                                this.source
                        ),
                        ex.getErrorOffset()
                );
                parseException.initCause(ex);
                throw parseException;
            }
            // else rethrow original exception
            throw ex;
        }
    }

    protected DateFormat getDateFormat(Locale locale) {
        return configureDateFormat(createDateFormat(locale));
    }

    private DateFormat configureDateFormat(DateFormat dateFormat) {
        if (this.timeZone != null) {
            dateFormat.setTimeZone(this.timeZone);
        }
        dateFormat.setLenient(this.lenient);
        return dateFormat;
    }

    private DateFormat createDateFormat(Locale locale) {
        if (StringUtils.hasLength(this.pattern)) {
            return new SimpleDateFormat(this.pattern, locale);
        }
        if (this.iso != null && this.iso != ISO.NONE) {
            String pattern = ISO_PATTERNS.get(this.iso);
            if (pattern == null) {
                throw new IllegalStateException("Unsupported ISO format " + this.iso);
            }
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setTimeZone(UTC);
            return format;
        }
        if (StringUtils.hasLength(this.stylePattern)) {
            int dateStyle = getStylePatternForChar(0);
            int timeStyle = getStylePatternForChar(1);
            if (dateStyle != -1 && timeStyle != -1) {
                return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
            }
            if (dateStyle != -1) {
                return DateFormat.getDateInstance(dateStyle, locale);
            }
            if (timeStyle != -1) {
                return DateFormat.getTimeInstance(timeStyle, locale);
            }
            throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");

        }
        return DateFormat.getDateInstance(this.style, locale);
    }

    private int getStylePatternForChar(int index) {
        if (this.stylePattern != null && this.stylePattern.length() > index) {
            switch (this.stylePattern.charAt(index)) {
                case 'S':
                    return DateFormat.SHORT;
                case 'M':
                    return DateFormat.MEDIUM;
                case 'L':
                    return DateFormat.LONG;
                case 'F':
                    return DateFormat.FULL;
                case '-':
                    return -1;
            }
        }
        throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
    }
}
