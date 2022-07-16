package org.clever.format.datetime.standard;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.time.Year;
import java.util.Locale;

/**
 * JSR-310 {@link Year}的格式化程序实现，遵循JSR-310的{@link Year}解析规则
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:32 <br/>
 */
class YearFormatter implements Formatter<Year> {
    @Override
    public Year parse(String text, Locale locale) throws ParseException {
        return Year.parse(text);
    }

    @Override
    public String print(Year object, Locale locale) {
        return object.toString();
    }
}
