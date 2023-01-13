package org.clever.format.support;

import org.clever.context.i18n.LocaleContextHolder;
import org.clever.format.Formatter;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

/**
 * 在 {@link Formatter} 和 {@link PropertyEditor} 之间架起桥梁的适配器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 21:58 <br/>
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {
    private final Formatter<Object> formatter;

    /**
     * 为给定的 {@link Formatter} 创建一个新的 {@code FormatterPropertyEditorAdapter}
     *
     * @param formatter {@link Formatter} 包装
     */
    @SuppressWarnings("unchecked")
    public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
        Assert.notNull(formatter, "Formatter must not be null");
        this.formatter = (Formatter<Object>) formatter;
    }

    /**
     * 确定 {@link Formatter} 声明的字段类型
     *
     * @return 在包装的 {@link Formatter} 实现中声明的字段类型（从不 {@code null}）
     * @throws IllegalArgumentException 如果无法推断 {@link Formatter} 声明的字段类型
     */
    public Class<?> getFieldType() {
        return FormattingConversionService.getFieldType(this.formatter);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            try {
                setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
            }
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
    }
}
