package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.util.Currency;

/**
 * 将字符串转换为{@link Currency}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:15 <br/>
 */
class StringToCurrencyConverter implements Converter<String, Currency> {
    @Override
    public Currency convert(String source) {
        return Currency.getInstance(source);
    }
}
