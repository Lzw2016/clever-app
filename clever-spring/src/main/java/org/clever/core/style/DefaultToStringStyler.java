package org.clever.core.style;

import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

/**
 * 默认{@code toString()}样式器。
 * <p>{@link ToStringCreator}使用该类根据约定以一致的方式设置{@code toString()}输出的样式。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:39 <br/>
 */
public class DefaultToStringStyler implements ToStringStyler {
    private final ValueStyler valueStyler;

    /**
     * 创建新的 DefaultToStringStyler.
     *
     * @param valueStyler 要使用的ValueStyler
     */
    public DefaultToStringStyler(ValueStyler valueStyler) {
        Assert.notNull(valueStyler, "ValueStyler must not be null");
        this.valueStyler = valueStyler;
    }

    /**
     * 返回此ToStringStyler使用的ValueStyler。
     */
    protected final ValueStyler getValueStyler() {
        return this.valueStyler;
    }

    @Override
    public void styleStart(StringBuilder buffer, Object obj) {
        if (!obj.getClass().isArray()) {
            buffer.append('[').append(ClassUtils.getShortName(obj.getClass()));
            styleIdentityHashCode(buffer, obj);
        } else {
            buffer.append('[');
            styleIdentityHashCode(buffer, obj);
            buffer.append(' ');
            styleValue(buffer, obj);
        }
    }

    private void styleIdentityHashCode(StringBuilder buffer, Object obj) {
        buffer.append('@');
        buffer.append(ObjectUtils.getIdentityHexString(obj));
    }

    @Override
    public void styleEnd(StringBuilder buffer, Object o) {
        buffer.append(']');
    }

    @Override
    public void styleField(StringBuilder buffer, String fieldName, Object value) {
        styleFieldStart(buffer, fieldName);
        styleValue(buffer, value);
        styleFieldEnd(buffer, fieldName);
    }

    protected void styleFieldStart(StringBuilder buffer, String fieldName) {
        buffer.append(' ').append(fieldName).append(" = ");
    }

    protected void styleFieldEnd(StringBuilder buffer, String fieldName) {
    }

    @Override
    public void styleValue(StringBuilder buffer, Object value) {
        buffer.append(this.valueStyler.style(value));
    }

    @Override
    public void styleFieldSeparator(StringBuilder buffer) {
        buffer.append(',');
    }
}
