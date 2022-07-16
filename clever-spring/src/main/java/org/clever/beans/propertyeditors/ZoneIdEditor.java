package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.time.ZoneId;

/**
 * {@code java.time.ZoneId} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:41 <br/>
 *
 * @see java.time.ZoneId
 * @see TimeZoneEditor
 */
public class ZoneIdEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(ZoneId.of(text));
    }

    @Override
    public String getAsText() {
        ZoneId value = (ZoneId) getValue();
        return (value != null ? value.getId() : "");
    }
}
