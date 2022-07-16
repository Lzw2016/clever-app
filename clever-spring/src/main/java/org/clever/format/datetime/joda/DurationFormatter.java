package org.clever.format.datetime.joda;

import org.clever.format.Formatter;
import org.joda.time.Duration;

import java.text.ParseException;
import java.util.Locale;

/**
 * Joda-Time {@link Duration} 格式化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:58 <br/>
 */
class DurationFormatter implements Formatter<Duration> {
    @Override
    public Duration parse(String text, Locale locale) throws ParseException {
        return Duration.parse(text);
    }

    @Override
    public String print(Duration object, Locale locale) {
        return object.toString();
    }
}
