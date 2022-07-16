package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

/**
 * char arrays 编辑器。字符串将简单地转换为相应的字符表示形式。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:43 <br/>
 *
 * @see String#toCharArray()
 */
public class CharArrayPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        setValue(text != null ? text.toCharArray() : null);
    }

    @Override
    public String getAsText() {
        char[] value = (char[]) getValue();
        return (value != null ? new String(value) : "");
    }
}
