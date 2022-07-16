package org.clever.context.i18n;

import org.clever.core.NamedInheritableThreadLocal;
import org.clever.core.NamedThreadLocal;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 用于设置或是读取当前线程的Locale TimeZone LocaleContext值的工具类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:00 <br/>
 */
public class LocaleContextHolder {
    /**
     * 当前线程的LocaleContext对象
     */
    private static final ThreadLocal<LocaleContext> localeContextHolder = new NamedThreadLocal<>("LocaleContext");
    /**
     * 继承自父线程的LocaleContext对象
     */
    private static final ThreadLocal<LocaleContext> inheritableLocaleContextHolder = new NamedInheritableThreadLocal<>("LocaleContext");
    /**
     * 共享的默认Locale值
     */
    private static Locale defaultLocale = Locale.CHINA;
    /**
     * 共享的默认TimeZone值
     */
    private static TimeZone defaultTimeZone = TimeZone.getTimeZone("GMT+:08:00");

    /**
     * 设置共享的默认Locale值
     */
    public static void setDefaultLocale(Locale locale) {
        defaultLocale = locale;
    }

    /**
     * 设置共享的默认TimeZone值
     */
    public static void setDefaultTimeZone(TimeZone timeZone) {
        defaultTimeZone = timeZone;
    }

    /**
     * 重置当前线程绑定的LocaleContext
     */
    public static void resetLocaleContext() {
        localeContextHolder.remove();
        inheritableLocaleContextHolder.remove();
    }

    /**
     * 设置当前线程绑定的LocaleContext
     *
     * @param localeContext 当前线程绑定的LocaleContext
     * @param inheritable   是否将LocaleContext传递到子线程
     */
    public static void setLocaleContext(LocaleContext localeContext, boolean inheritable) {
        if (localeContext == null) {
            resetLocaleContext();
        } else {
            if (inheritable) {
                inheritableLocaleContextHolder.set(localeContext);
                localeContextHolder.remove();
            } else {
                localeContextHolder.set(localeContext);
                inheritableLocaleContextHolder.remove();
            }
        }
    }

    /**
     * 设置当前线程绑定的Locale
     */
    public static void setLocale(Locale locale) {
        setLocale(locale, false);
    }

    /**
     * 设置当前线程绑定的Locale
     *
     * @param locale      当前线程绑定的Locale
     * @param inheritable 是否将Locale传递到子线程
     */
    public static void setLocale(Locale locale, boolean inheritable) {
        LocaleContext localeContext = getLocaleContext();
        TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ? ((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
        if (timeZone != null) {
            localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
        } else if (locale != null) {
            localeContext = new SimpleLocaleContext(locale);
        } else {
            localeContext = null;
        }
        setLocaleContext(localeContext, inheritable);
    }

    /**
     * 设置当前线程绑定的TimeZone
     */
    public static void setTimeZone(TimeZone timeZone) {
        setTimeZone(timeZone, false);
    }

    /**
     * 设置当前线程绑定的TimeZone
     *
     * @param timeZone    当前线程绑定的TimeZone
     * @param inheritable 是否将TimeZone传递到子线程
     */
    public static void setTimeZone(TimeZone timeZone, boolean inheritable) {
        LocaleContext localeContext = getLocaleContext();
        Locale locale = (localeContext != null ? localeContext.getLocale() : null);
        if (timeZone != null) {
            localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
        } else if (locale != null) {
            localeContext = new SimpleLocaleContext(locale);
        } else {
            localeContext = null;
        }
        setLocaleContext(localeContext, inheritable);
    }

    /**
     * 返回当前线程绑定的LocaleContext对象
     */
    public static LocaleContext getLocaleContext() {
        LocaleContext localeContext = localeContextHolder.get();
        if (localeContext == null) {
            localeContext = inheritableLocaleContextHolder.get();
        }
        return localeContext;
    }

    /**
     * 返回当前环境的Locale，如果当前环境未设置Locale，则返回默认Locale值
     */
    public static Locale getLocale() {
        return getLocale(getLocaleContext());
    }

    /**
     * 返回当前环境的Locale，如果当前环境未设置Locale，则返回默认Locale值
     */
    public static Locale getLocale(LocaleContext localeContext) {
        if (localeContext != null) {
            Locale locale = localeContext.getLocale();
            if (locale != null) {
                return locale;
            }
        }
        return (defaultLocale != null ? defaultLocale : Locale.getDefault());
    }

    /**
     * 返回当前环境的TimeZone，如果当前环境未设置TimeZone，则返回默认TimeZone值
     */
    public static TimeZone getTimeZone() {
        return getTimeZone(getLocaleContext());
    }

    /**
     * 返回当前环境的TimeZone，如果当前环境未设置TimeZone，则返回默认TimeZone值
     */
    public static TimeZone getTimeZone(LocaleContext localeContext) {
        if (localeContext instanceof TimeZoneAwareLocaleContext) {
            TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
            if (timeZone != null) {
                return timeZone;
            }
        }
        return (defaultTimeZone != null ? defaultTimeZone : TimeZone.getDefault());
    }
}
