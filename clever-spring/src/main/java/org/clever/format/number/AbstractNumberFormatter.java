package org.clever.format.number;

import org.clever.format.Formatter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * 抽象的数字格式化，提供{@link #getNumberFormat(java.util.Locale)}模板方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:40 <br/>
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {
    /**
     * 是否宽松模式
     */
    private boolean lenient = false;

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    @Override
    public String print(Number number, Locale locale) {
        return getNumberFormat(locale).format(number);
    }

    @Override
    public Number parse(String text, Locale locale) throws ParseException {
        NumberFormat format = getNumberFormat(locale);
        ParsePosition position = new ParsePosition(0);
        Number number = format.parse(text, position);
        if (position.getErrorIndex() != -1) {
            throw new ParseException(text, position.getIndex());
        }
        if (!this.lenient) {
            if (text.length() != position.getIndex()) {
                // indicates a part of the string that was not parsed
                throw new ParseException(text, position.getIndex());
            }
        }
        return number;
    }

    /**
     * 获取指定区域设置的具体数字格式
     *
     * @param locale 当前区域设置
     */
    protected abstract NumberFormat getNumberFormat(Locale locale);
}
