package org.clever.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

/**
 * TimeZoneAwareLocaleContext接口的简单实现，，总是返回指定的Locale和TimeZone
 * 作者：lizw <br/>
 * 创建时间：2022/05/09 20:00 <br/>
 */
public class SimpleTimeZoneAwareLocaleContext extends SimpleLocaleContext implements TimeZoneAwareLocaleContext {
    private final TimeZone timeZone;

    public SimpleTimeZoneAwareLocaleContext(Locale locale, TimeZone timeZone) {
        super(locale);
        this.timeZone = timeZone;
    }

    @Override
    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    @Override
    public String toString() {
        return super.toString() + " " + (this.timeZone != null ? this.timeZone.toString() : "-");
    }
}
