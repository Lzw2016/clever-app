package org.clever.format.datetime.joda;

import org.clever.context.i18n.LocaleContext;
import org.clever.context.i18n.LocaleContextHolder;
import org.clever.context.i18n.TimeZoneAwareLocaleContext;
import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import java.util.TimeZone;

/**
 * 保存用户特定Joda-Time设置的上下文，如用户的日历系统和时区。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:49 <br/>
 */
public class JodaTimeContext {
    /**
     * 日历系统
     */
    private Chronology chronology;
    /**
     * 时区
     */
    private DateTimeZone timeZone;

    public void setChronology(Chronology chronology) {
        this.chronology = chronology;
    }

    public Chronology getChronology() {
        return this.chronology;
    }

    /**
     * @see org.clever.context.i18n.LocaleContextHolder#getTimeZone()
     * @see org.clever.context.i18n.LocaleContextHolder#setLocaleContext
     */
    public void setTimeZone(DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    /**
     * 获取DateTimeFormatter，并将此上下文的设置应用于基本格式化程序
     *
     * @param formatter 建立默认格式规则的基本格式设置程序，通常与上下文无关
     */
    public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
        if (this.chronology != null) {
            formatter = formatter.withChronology(this.chronology);
        }
        if (this.timeZone != null) {
            formatter = formatter.withZone(this.timeZone);
        } else {
            LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
            if (localeContext instanceof TimeZoneAwareLocaleContext) {
                TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
                if (timeZone != null) {
                    formatter = formatter.withZone(DateTimeZone.forTimeZone(timeZone));
                }
            }
        }
        return formatter;
    }
}
