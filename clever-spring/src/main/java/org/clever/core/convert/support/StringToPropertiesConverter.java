package org.clever.core.convert.support;

import org.clever.core.convert.converter.Converter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 通过调用{@link Properties#load(java.io.InputStream)}将字符串转换为属性。
 * 使用属性所需的{@code ISO-8559-1}编码
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:17 <br/>
 */
final class StringToPropertiesConverter implements Converter<String, Properties> {
    @Override
    public Properties convert(String source) {
        try {
            Properties props = new Properties();
            // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
            props.load(new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));
            return props;
        } catch (Exception ex) {
            // Should never happen.
            throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
        }
    }
}
