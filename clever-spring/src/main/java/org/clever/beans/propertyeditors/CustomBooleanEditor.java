package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Boolean/boolean 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:44 <br/>
 *
 * @see org.clever.validation.DataBinder#registerCustomEditor
 */
public class CustomBooleanEditor extends PropertyEditorSupport {
    public static final String VALUE_TRUE = "true";
    public static final String VALUE_FALSE = "false";
    public static final String VALUE_ON = "on";
    public static final String VALUE_OFF = "off";
    public static final String VALUE_YES = "yes";
    public static final String VALUE_NO = "no";
    public static final String VALUE_1 = "1";
    public static final String VALUE_0 = "0";

    private final String trueString;
    private final String falseString;
    private final boolean allowEmpty;

    /**
     * 创建一个新的CustomBooleanEditor实例，将"true"/"on"/"yes"和"false"/"off"/"no"作为可识别的字符串值。
     * "allowEmpty"参数说明是否应允许空字符串进行解析，即将其解释为null值。
     * 否则，在这种情况下会抛出IllegalArgumentException
     *
     * @param allowEmpty 如果应允许空字符串
     */
    public CustomBooleanEditor(boolean allowEmpty) {
        this(null, null, allowEmpty);
    }

    /**
     * 创建一个新的CustomBooleanEditor实例，该实例具有可配置的字符串值true和false。
     * "allowEmpty"参数说明是否应允许空字符串进行解析，即将其解释为null值。
     * 否则，在这种情况下会抛出IllegalArgumentException
     *
     * @param trueString  表示true的字符串值
     * @param falseString 表示false的字符串值
     * @param allowEmpty  如果应允许空字符串
     * @see #VALUE_TRUE
     * @see #VALUE_FALSE
     * @see #VALUE_ON
     * @see #VALUE_OFF
     * @see #VALUE_YES
     * @see #VALUE_NO
     */
    public CustomBooleanEditor(String trueString, String falseString, boolean allowEmpty) {
        this.trueString = trueString;
        this.falseString = falseString;
        this.allowEmpty = allowEmpty;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String input = (text != null ? text.trim() : null);
        if (this.allowEmpty && !StringUtils.hasLength(input)) {
            // Treat empty String as null value.
            setValue(null);
        } else if (this.trueString != null && this.trueString.equalsIgnoreCase(input)) {
            setValue(Boolean.TRUE);
        } else if (this.falseString != null && this.falseString.equalsIgnoreCase(input)) {
            setValue(Boolean.FALSE);
        } else if (this.trueString == null
                && (VALUE_TRUE.equalsIgnoreCase(input)
                || VALUE_ON.equalsIgnoreCase(input)
                || VALUE_YES.equalsIgnoreCase(input)
                || VALUE_1.equals(input))) {
            setValue(Boolean.TRUE);
        } else if (this.falseString == null
                && (VALUE_FALSE.equalsIgnoreCase(input)
                || VALUE_OFF.equalsIgnoreCase(input)
                || VALUE_NO.equalsIgnoreCase(input)
                || VALUE_0.equals(input))) {
            setValue(Boolean.FALSE);
        } else {
            throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
        }
    }

    @Override
    public String getAsText() {
        if (Boolean.TRUE.equals(getValue())) {
            return (this.trueString != null ? this.trueString : VALUE_TRUE);
        } else if (Boolean.FALSE.equals(getValue())) {
            return (this.falseString != null ? this.falseString : VALUE_FALSE);
        } else {
            return "";
        }
    }
}
