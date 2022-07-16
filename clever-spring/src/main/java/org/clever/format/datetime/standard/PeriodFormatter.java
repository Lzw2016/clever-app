package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.Period;
import java.util.Locale;

/**
 * JSR-310 {@link Period}的格式化程序实现，遵循JSR-310的{@link Period}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:31 <br/>
 */
class PeriodFormatter implements Formatter<Period> {
    @Override
    public Period parse(String text, Locale locale) throws ParseException {
        return Period.parse(text);
    }

    @Override
    public String print(Period object, Locale locale) {
        return object.toString();
    }
}
