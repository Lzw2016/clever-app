package org.clever.format.datetime.standard;

import org.clever.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * 内部的 {@link DateTimeFormatter} 工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:05 <br/>
 */
public class DateTimeFormatterUtils {
    static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
        // Using strict parsing to align with Joda-Time and standard DateFormat behavior:
        // otherwise, an overflow like e.g. Feb 29 for a non-leap-year wouldn't get rejected.
        // However, with strict parsing, a year digit needs to be specified as 'u'...
        String patternToUse = StringUtils.replace(pattern, "yy", "uu");
        return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
    }
}
