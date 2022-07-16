package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

/**
 * {@code java.nio.charset.Charset} 编辑器，将字符集字符串表示转换为字符集对象并返回。
 * 需要与字符集的字符集相同的语法{@link java.nio.charset.Charset#name()}，
 * 例如{@code UTF-8}, {@code ISO-8859-16}等
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:27 <br/>
 *
 * @see Charset
 */
public class CharsetEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            setValue(Charset.forName(text));
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Charset value = (Charset) getValue();
        return (value != null ? value.name() : "");
    }
}
