package org.clever.core.style;

import org.clever.util.Assert;

/**
 * 实用程序类，该类使用可插入的样式约定构建漂亮的打印{@code toString()}方法。
 * 默认情况下，ToStringCreator遵循的{@code toString()}样式约定。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:38 <br/>
 */
public class ToStringCreator {
    /**
     * 此ToString Creator使用的默认ToString Styler实例。
     */
    private static final ToStringStyler DEFAULT_TO_STRING_STYLER = new DefaultToStringStyler(StylerUtils.DEFAULT_VALUE_STYLER);

    private final StringBuilder buffer = new StringBuilder(256);
    private final ToStringStyler styler;
    private final Object object;
    private boolean styledFirstField;

    /**
     * 为给定对象创建ToStringCreator。
     *
     * @param obj 要字符串化的对象
     */
    public ToStringCreator(Object obj) {
        this(obj, (ToStringStyler) null);
    }

    /**
     * 使用提供的样式为给定对象创建ToStringCreator。
     *
     * @param obj    要字符串化的对象
     * @param styler ValueStyler封装了漂亮的打印指令
     */
    public ToStringCreator(Object obj, ValueStyler styler) {
        this(obj, new DefaultToStringStyler(styler != null ? styler : StylerUtils.DEFAULT_VALUE_STYLER));
    }

    /**
     * 使用提供的样式为给定对象创建ToStringCreator。
     *
     * @param obj    要字符串化的对象
     * @param styler 封装漂亮打印指令的ToStringStyler
     */
    public ToStringCreator(Object obj, ToStringStyler styler) {
        Assert.notNull(obj, "The object to be styled must not be null");
        this.object = obj;
        this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
        this.styler.styleStart(this.buffer, this.object);
    }

    /**
     * 追加byte字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, byte value) {
        return append(fieldName, Byte.valueOf(value));
    }

    /**
     * 附加short字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, short value) {
        return append(fieldName, Short.valueOf(value));
    }

    /**
     * 追加int字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, int value) {
        return append(fieldName, Integer.valueOf(value));
    }

    /**
     * 附加long字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, long value) {
        return append(fieldName, Long.valueOf(value));
    }

    /**
     * 附加float字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, float value) {
        return append(fieldName, Float.valueOf(value));
    }

    /**
     * 附加double字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     the field value
     * @return this, to support call-chaining
     */
    public ToStringCreator append(String fieldName, double value) {
        return append(fieldName, Double.valueOf(value));
    }

    /**
     * 附加boolean字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, boolean value) {
        return append(fieldName, Boolean.valueOf(value));
    }

    /**
     * 附加字段值。
     *
     * @param fieldName 字段的名称，通常是成员变量名
     * @param value     字段值
     * @return 这是为了支持调用链接
     */
    public ToStringCreator append(String fieldName, Object value) {
        printFieldSeparatorIfNecessary();
        this.styler.styleField(this.buffer, fieldName, value);
        return this;
    }

    private void printFieldSeparatorIfNecessary() {
        if (this.styledFirstField) {
            this.styler.styleFieldSeparator(this.buffer);
        } else {
            this.styledFirstField = true;
        }
    }

    /**
     * 附加提供的值。
     *
     * @param value 要附加的值
     * @return 这是为了支持调用链接。
     */
    public ToStringCreator append(Object value) {
        this.styler.styleValue(this.buffer, value);
        return this;
    }

    /**
     * 返回此ToStringCreator生成的字符串表示形式。
     */
    @Override
    public String toString() {
        this.styler.styleEnd(this.buffer, this.object);
        return this.buffer.toString();
    }
}
