package org.clever.format.datetime.standard;

import org.clever.context.i18n.LocaleContext;
import org.clever.context.i18n.LocaleContextHolder;
import org.clever.context.i18n.TimeZoneAwareLocaleContext;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 保存用户特定java的上下文{@code java.time}(JSR-310)设置，如用户的日历系统和时区。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:24 <br/>
 */
public class DateTimeContext {
    /**
     * 日历系统
     */
    private Chronology chronology;
    /**
     * 时区
     */
    private ZoneId timeZone;

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
    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    public ZoneId getTimeZone() {
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
                    formatter = formatter.withZone(timeZone.toZoneId());
                }
            }
        }
        return formatter;
    }
}
