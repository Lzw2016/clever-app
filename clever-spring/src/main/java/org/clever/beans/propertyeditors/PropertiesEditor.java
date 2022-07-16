package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * 自定义 {@link java.beans.PropertyEditor} 为 {@link Properties} 对象.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:37 <br/>
 *
 * @see java.util.Properties#load
 */
public class PropertiesEditor extends PropertyEditorSupport {
    /**
     * 将{@link String}转换为{@link Properties}，将其视为属性内容
     *
     * @param text 要如此转换的文本
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        Properties props = new Properties();
        if (text != null) {
            try {
                // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
                props.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
            } catch (IOException ex) {
                // Should never happen.
                throw new IllegalArgumentException("Failed to parse [" + text + "] into Properties", ex);
            }
        }
        setValue(props);
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Properties) && value instanceof Map) {
            Properties props = new Properties();
            props.putAll((Map<?, ?>) value);
            super.setValue(props);
        } else {
            super.setValue(value);
        }
    }
}
