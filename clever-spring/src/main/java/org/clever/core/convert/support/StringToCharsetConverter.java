package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.nio.charset.Charset;

/**
 * 将字符串转换为字符集
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:16 <br/>
 */
class StringToCharsetConverter implements Converter<String, Charset> {
    @Override
    public Charset convert(String source) {
        return Charset.forName(source);
    }
}
