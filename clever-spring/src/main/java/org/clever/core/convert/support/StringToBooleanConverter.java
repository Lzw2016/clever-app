package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.util.HashSet;
import java.util.Set;

/**
 * 将字符串转换为布尔值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:06 <br/>
 */
final class StringToBooleanConverter implements Converter<String, Boolean> {
    private static final Set<String> trueValues = new HashSet<>(8);
    private static final Set<String> falseValues = new HashSet<>(8);

    static {
        trueValues.add("true");
        trueValues.add("on");
        trueValues.add("yes");
        trueValues.add("1");

        falseValues.add("false");
        falseValues.add("off");
        falseValues.add("no");
        falseValues.add("0");
    }

    @Override
    public Boolean convert(String source) {
        String value = source.trim();
        if (value.isEmpty()) {
            return null;
        }
        value = value.toLowerCase();
        if (trueValues.contains(value)) {
            return Boolean.TRUE;
        } else if (falseValues.contains(value)) {
            return Boolean.FALSE;
        } else {
            throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
        }
    }
}
