package org.clever.beans.propertyeditors;

import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * 自定义字符串数组 {@link java.beans.PropertyEditor}<br/>
 * 字符串必须为CSV格式，并带有可自定义的分隔符。默认情况下，结果中的值将被删除空白
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:48 <br/>
 *
 * @see org.clever.util.StringUtils#delimitedListToStringArray
 * @see org.clever.util.StringUtils#arrayToDelimitedString
 */
public class StringArrayPropertyEditor extends PropertyEditorSupport {
    /**
     * 拆分字符串的默认分隔符：逗号(",").
     */
    public static final String DEFAULT_SEPARATOR = ",";

    private final String separator;
    private final String charsToDelete;
    private final boolean emptyArrayAsNull;
    private final boolean trimValues;

    /**
     * 使用默认分隔符（逗号）创建新的StringArrayPropertyEditor。
     * 空文本（无元素）将转换为空数组
     */
    public StringArrayPropertyEditor() {
        this(DEFAULT_SEPARATOR, null, false);
    }

    /**
     * @param separator 用于拆分字符串的分隔符
     */
    public StringArrayPropertyEditor(String separator) {
        this(separator, null, false);
    }

    /**
     * @param separator        用于拆分字符串的分隔符
     * @param emptyArrayAsNull 如果要将空字符串数组转换为null，则为true
     */
    public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
        this(separator, null, emptyArrayAsNull);
    }

    /**
     * @param separator        用于拆分字符串的分隔符
     * @param emptyArrayAsNull 如果要将空字符串数组转换为null，则为true
     * @param trimValues       如果要修剪已解析数组中的值的空格，则为true（默认值为true）
     */
    public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
        this(separator, null, emptyArrayAsNull, trimValues);
    }

    /**
     * @param separator        用于拆分字符串的分隔符
     * @param charsToDelete    除了修剪输入字符串之外，还需要删除的一组字符。用于删除不需要的换行符：例如"\r\n\f"将删除字符串中的所有新行和换行符
     * @param emptyArrayAsNull 如果要将空字符串数组转换为null，则为true
     */
    public StringArrayPropertyEditor(String separator, String charsToDelete, boolean emptyArrayAsNull) {
        this(separator, charsToDelete, emptyArrayAsNull, true);
    }

    /**
     * 使用给定的分隔符创建新的StringArrayPropertyEditor
     *
     * @param separator        用于拆分字符串的分隔符
     * @param charsToDelete    除了修剪输入字符串之外，还需要删除的一组字符。用于删除不需要的换行符：例如"\r\n\f"将删除字符串中的所有新行和换行符
     * @param emptyArrayAsNull 如果要将空字符串数组转换为null，则为true
     * @param trimValues       如果要修剪已解析数组中的值的空格，则为true（默认值为true）
     */
    public StringArrayPropertyEditor(String separator, String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {
        this.separator = separator;
        this.charsToDelete = charsToDelete;
        this.emptyArrayAsNull = emptyArrayAsNull;
        this.trimValues = trimValues;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
        if (this.emptyArrayAsNull && array.length == 0) {
            setValue(null);
        } else {
            if (this.trimValues) {
                array = StringUtils.trimArrayElements(array);
            }
            setValue(array);
        }
    }

    @Override
    public String getAsText() {
        return StringUtils.arrayToDelimitedString(ObjectUtils.toObjectArray(getValue()), this.separator);
    }
}
