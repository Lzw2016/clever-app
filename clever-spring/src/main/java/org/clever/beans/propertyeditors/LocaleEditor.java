package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@code java.util.Locale} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:33 <br/>
 *
 * @see java.util.Locale
 * @see org.clever.util.StringUtils#parseLocaleString
 */
public class LocaleEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        setValue(StringUtils.parseLocaleString(text));
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? value.toString() : "");
    }
}
