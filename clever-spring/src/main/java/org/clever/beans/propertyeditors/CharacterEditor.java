package org.clever.beans.propertyeditors;

import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link Character} 编辑器。
 * 还支持从Unicode字符序列转换，如: {@code u0041} ('A')
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:44 <br/>
 *
 * @see Character
 * @see org.clever.beans.BeanWrapperImpl
 */
public class CharacterEditor extends PropertyEditorSupport {
    /**
     * 将字符串标识为Unicode字符序列的前缀
     */
    private static final String UNICODE_PREFIX = "\\u";
    /**
     * Unicode字符序列的长度
     */
    private static final int UNICODE_LENGTH = 6;
    private final boolean allowEmpty;

    /**
     * 创建新的CharacterEditor实例。
     * "allowEmpty"参数控制是否允许在解析中使用空字符串，即在转换文本时将其解释为空值。
     * 如果为false，则此时将抛出 {@link IllegalArgumentException}
     *
     * @param allowEmpty 如果允许空字符串
     */
    public CharacterEditor(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.allowEmpty && !StringUtils.hasLength(text)) {
            // Treat empty String as null value.
            setValue(null);
        } else if (text == null) {
            throw new IllegalArgumentException("null String cannot be converted to char type");
        } else if (isUnicodeCharacterSequence(text)) {
            setAsUnicode(text);
        } else if (text.length() == 1) {
            setValue(text.charAt(0));
        } else {
            throw new IllegalArgumentException(
                    "String [" + text + "] with length " + text.length() + " cannot be converted to char type: neither Unicode nor single character"
            );
        }
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? value.toString() : "");
    }

    private boolean isUnicodeCharacterSequence(String sequence) {
        return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
    }

    private void setAsUnicode(String text) {
        int code = Integer.parseInt(text.substring(UNICODE_PREFIX.length()), 16);
        setValue((char) code);
    }
}
