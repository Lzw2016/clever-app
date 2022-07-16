package org.clever.format.datetime.joda;

import org.clever.format.Formatter;
import org.joda.time.MonthDay;

import java.text.ParseException;
import java.util.Locale;

/**
 * Joda Time{@link MonthDay}的格式化程序实现，遵循Joda Time对{@link MonthDay}的解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 21:00 <br/>
 */
class MonthDayFormatter implements Formatter<MonthDay> {
    @Override
    public MonthDay parse(String text, Locale locale) throws ParseException {
        return MonthDay.parse(text);
    }

    @Override
    public String print(MonthDay object, Locale locale) {
        return object.toString();
    }
}
