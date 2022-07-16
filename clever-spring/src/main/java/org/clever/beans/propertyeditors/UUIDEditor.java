package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.UUID;

/**
 * {@code java.util.UUID} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:41 <br/>
 *
 * @see java.util.UUID
 */
public class UUIDEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            setValue(UUID.fromString(text.trim()));
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        UUID value = (UUID) getValue();
        return (value != null ? value.toString() : "");
    }
}
