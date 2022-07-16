package org.clever.context.i18n;

import java.util.Locale;

/**
 * LocaleContext接口的简单实现，总是返回指定的Locale
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/09 13:28 <br/>
 */
public class SimpleLocaleContext implements LocaleContext {
    private final Locale locale;

    public SimpleLocaleContext(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return (this.locale != null ? this.locale.toString() : "-");
    }
}
