package org.clever.format.datetime.joda;

import org.clever.format.Formatter;
import org.joda.time.Period;

import java.text.ParseException;
import java.util.Locale;

/**
 * Joda Time{@link Period}的格式化程序实现，遵循Joda Time对{@link Period}的解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:58 <br/>
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
