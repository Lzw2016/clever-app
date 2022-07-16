package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.MonthDay;
import java.util.Locale;

/**
 * JSR-310 {@link MonthDay}的格式化程序实现，遵循JSR-310的{@link MonthDay}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:33 <br/>
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
