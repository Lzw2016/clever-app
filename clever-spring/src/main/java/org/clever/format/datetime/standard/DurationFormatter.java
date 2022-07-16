package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

/**
 * JSR-310 {@link Duration}的格式化程序实现，遵循JSR-310 {@link Duration}的解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:31 <br/>
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
