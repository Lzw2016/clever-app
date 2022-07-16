package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 通过调用{@link Properties#store(java.io.OutputStream, String)}将属性转换为字符串。在返回字符串之前，使用{@code ISO-8859-1}字符集进行解码
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:18 <br/>
 */
final class PropertiesToStringConverter implements Converter<Properties, String> {
    @Override
    public String convert(Properties source) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(256);
            source.store(os, null);
            return os.toString("ISO-8859-1");
        } catch (IOException ex) {
            // Should never happen.
            throw new IllegalArgumentException("Failed to store [" + source + "] into String", ex);
        }
    }
}
