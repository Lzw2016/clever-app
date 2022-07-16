package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.YearMonth;
import java.util.Locale;

/**
 * JSR-310 {@link YearMonth}的格式化程序实现，遵循JSR-310的{@link YearMonth}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:33 <br/>
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
