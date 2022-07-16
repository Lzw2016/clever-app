package org.clever.boot.convert;

import org.clever.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * 格式化 {@code char[]}，使用 {@link Formatter}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:02 <br/>
 */
final class CharArrayFormatter implements Formatter<char[]> {
    @Override
    public String print(char[] object, Locale locale) {
        return new String(object);
    }

    @Override
    public char[] parse(String text, Locale locale) throws ParseException {
        return text.toCharArray();
    }
}
