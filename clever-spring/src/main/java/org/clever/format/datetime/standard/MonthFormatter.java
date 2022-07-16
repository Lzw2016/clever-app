package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.Month;
import java.util.Locale;

/**
 * JSR-310 {@link Month}的格式化程序实现，遵循JSR-310的{@link Month}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:32 <br/>
 */
class MonthFormatter implements Formatter<Month> {
    @Override
    public Month parse(String text, Locale locale) throws ParseException {
        return Month.valueOf(text.toUpperCase());
    }

    @Override
    public String print(Month object, Locale locale) {
        return object.toString();
    }
}
