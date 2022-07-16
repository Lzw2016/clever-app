package org.clever.format.datetime.joda;

import org.clever.core.NamedThreadLocal;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * 线程本地{@link JodaTimeContext}的持有者，具有用户特定的Joda-Time设置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:49 <br/>
 */
public final class JodaTimeContextHolder {
    private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder = new NamedThreadLocal<>("JodaTimeContext");

    private JodaTimeContextHolder() {
    }

    /**
     * 重置当前线程的JodaTimeContext
     */
    public static void resetJodaTimeContext() {
        jodaTimeContextHolder.remove();
    }

    /**
     * 将给定的JodaTimeContext与当前线程关联
     */
    public static void setJodaTimeContext(JodaTimeContext jodaTimeContext) {
        if (jodaTimeContext == null) {
            resetJodaTimeContext();
        } else {
            jodaTimeContextHolder.set(jodaTimeContext);
        }
    }

    /**
     * 返回与当前线程关联的JodaTimeContext
     */
    public static JodaTimeContext getJodaTimeContext() {
        return jodaTimeContextHolder.get();
    }

    /**
     * 获取DateTimeFormatter，并将特定于用户的设置应用于给定的基本格式化程序。
     *
     * @param formatter 建立默认格式规则的基本格式设置程序(通常独立于用户)
     * @param locale    当前用户区域设置(如果未知，则可能为null)
     */
    public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
        DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
        JodaTimeContext context = getJodaTimeContext();
        return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
    }
}
