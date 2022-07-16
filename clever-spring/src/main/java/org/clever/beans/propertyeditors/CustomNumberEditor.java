package org.clever.beans.propertyeditors;

import org.clever.util.NumberUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.text.NumberFormat;

/**
 * 任何数字子类（如Short、Integer、Long、BigInteger、Float、Double、BigDecimal）编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:46 <br/>
 *
 * @see Number
 * @see java.text.NumberFormat
 * @see org.clever.validation.DataBinder#registerCustomEditor
 */
public class CustomNumberEditor extends PropertyEditorSupport {
    private final Class<? extends Number> numberClass;
    private final NumberFormat numberFormat;
    private final boolean allowEmpty;

    /**
     * @param numberClass 要生成的Number子类
     * @param allowEmpty  如果应允许空字符串
     * @throws IllegalArgumentException 如果指定了无效的numberClass
     * @see org.clever.util.NumberUtils#parseNumber(String, Class)
     * @see Integer#valueOf
     * @see Integer#toString
     */
    public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
        this(numberClass, null, allowEmpty);
    }

    /**
     * @param numberClass  要生成的Number子类
     * @param numberFormat 用于解析和呈现的NumberFormat
     * @param allowEmpty   如果应允许空字符串
     * @throws IllegalArgumentException 如果指定了无效的numberClass
     * @see org.clever.util.NumberUtils#parseNumber(String, Class, java.text.NumberFormat)
     * @see java.text.NumberFormat#parse
     * @see java.text.NumberFormat#format
     */
    public CustomNumberEditor(Class<? extends Number> numberClass, NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {
        if (!Number.class.isAssignableFrom(numberClass)) {
            throw new IllegalArgumentException("Property class must be a subclass of Number");
        }
        this.numberClass = numberClass;
        this.numberFormat = numberFormat;
        this.allowEmpty = allowEmpty;
    }

    /**
     * 使用指定的NumberFormat分析给定文本中的数字
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.allowEmpty && !StringUtils.hasText(text)) {
            // Treat empty String as null value.
            setValue(null);
        } else if (this.numberFormat != null) {
            // Use given NumberFormat for parsing text.
            setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
        } else {
            // Use default valueOf methods for parsing text.
            setValue(NumberUtils.parseNumber(text, this.numberClass));
        }
    }

    /**
     * 如有必要，将数值强制到所需的目标类中
     */
    @Override
    public void setValue(Object value) {
        if (value instanceof Number) {
            super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
        } else {
            super.setValue(value);
        }
    }

    /**
     * 使用指定的NumberFormat将数字格式化为字符串
     */
    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }
        if (this.numberFormat != null) {
            // Use NumberFormat for rendering value.
            return this.numberFormat.format(value);
        } else {
            // Use toString method for rendering value.
            return value.toString();
        }
    }
}
