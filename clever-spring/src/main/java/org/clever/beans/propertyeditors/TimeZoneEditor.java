package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.TimeZone;

/**
 * {@code java.util.TimeZone} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:39 <br/>
 *
 * @see java.util.TimeZone
 * @see ZoneIdEditor
 */
public class TimeZoneEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(StringUtils.parseTimeZoneString(text));
    }

    @Override
    public String getAsText() {
        TimeZone value = (TimeZone) getValue();
        return (value != null ? value.getID() : "");
    }
}
