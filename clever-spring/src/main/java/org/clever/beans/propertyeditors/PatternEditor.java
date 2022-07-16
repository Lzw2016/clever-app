package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

/**
 * {@code java.util.regex.Pattern} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:36 <br/>
 *
 * @see java.util.regex.Pattern
 * @see java.util.regex.Pattern#compile(String)
 */
public class PatternEditor extends PropertyEditorSupport {
    private final int flags;

    /**
     * 使用默认设置创建新的PatternEditor
     */
    public PatternEditor() {
        this.flags = 0;
    }

    /**
     * 使用给定的设置创建新的PatternEditor
     *
     * @param flags {@code java.util.regex.Pattern} 要应用的模式标志
     * @see java.util.regex.Pattern#compile(String, int)
     * @see java.util.regex.Pattern#CASE_INSENSITIVE
     * @see java.util.regex.Pattern#MULTILINE
     * @see java.util.regex.Pattern#DOTALL
     * @see java.util.regex.Pattern#UNICODE_CASE
     * @see java.util.regex.Pattern#CANON_EQ
     */
    public PatternEditor(int flags) {
        this.flags = flags;
    }

    @Override
    public void setAsText(String text) {
        setValue(text != null ? Pattern.compile(text, this.flags) : null);
    }

    @Override
    public String getAsText() {
        Pattern value = (Pattern) getValue();
        return (value != null ? value.pattern() : "");
    }
}
