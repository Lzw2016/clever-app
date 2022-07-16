package org.clever.format.datetime.standard;

import org.clever.core.NamedThreadLocal;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 线程本地的{@link DateTimeContext}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:23 <br/>
 */
public final class DateTimeContextHolder {
    private static final ThreadLocal<DateTimeContext> dateTimeContextHolder = new NamedThreadLocal<>("DateTimeContext");

    private DateTimeContextHolder() {
    }

    /**
     * 重置当前线程的DateTimeContext
     */
    public static void resetDateTimeContext() {
        dateTimeContextHolder.remove();
    }

    /**
     * 将给定的DateTimeContext与当前线程关联
     */
    public static void setDateTimeContext(DateTimeContext dateTimeContext) {
        if (dateTimeContext == null) {
            resetDateTimeContext();
        } else {
            dateTimeContextHolder.set(dateTimeContext);
        }
    }

    /**
     * 返回与当前线程关联的DateTimeContext
     */
    public static DateTimeContext getDateTimeContext() {
        return dateTimeContextHolder.get();
    }

    /**
     * 获取DateTimeFormatter，并将特定于用户的设置应用于给定的基本格式化程序。
     *
     * Obtain a DateTimeFormatter with user-specific settings applied to the given base formatter.
     *
     * @param formatter 建立默认格式规则的基本格式设置程序(通常独立于用户)
     * @param locale    当前用户区域设置(如果未知，则可能为null)
     */
    public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
        DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
        DateTimeContext context = getDateTimeContext();
        return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
    }
}
