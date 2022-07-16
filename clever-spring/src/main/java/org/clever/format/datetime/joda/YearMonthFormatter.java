package org.clever.format.datetime.joda;

import org.clever.format.Formatter;
import org.joda.time.YearMonth;

import java.text.ParseException;
import java.util.Locale;

/**
 * Joda Time {@link YearMonth}的格式化程序实现，遵循Joda Time对{@link YearMonth}的解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:59 <br/>
 */
class YearMonthFormatter implements Formatter<YearMonth> {
    @Override
    public YearMonth parse(String text, Locale locale) throws ParseException {
        return YearMonth.parse(text);
    }

    @Override
    public String print(YearMonth object, Locale locale) {
        return object.toString();
    }
}
