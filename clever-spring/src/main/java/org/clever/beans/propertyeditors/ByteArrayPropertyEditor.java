package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

/**
 * byte arrays编辑器。字符串将简单地转换为相应的字节表示形式。
 *
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:43 <br/>
 * @see java.lang.String#getBytes
 */
public class ByteArrayPropertyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText( String text) {
        setValue(text != null ? text.getBytes() : null);
    }

    @Override
    public String getAsText() {
        byte[] value = (byte[]) getValue();
        return (value != null ? new String(value) : "");
    }
}
