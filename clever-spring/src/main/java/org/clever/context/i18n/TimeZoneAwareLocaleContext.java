package org.clever.context.i18n;

import java.util.TimeZone;

/**
 * 用于获取当前环境Locale对象和TimeZone对象的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 20:26 <br/>
 */
public interface TimeZoneAwareLocaleContext extends LocaleContext {
    /**
     * 返回当前的TimeZone，可以是固定的，也可以动态获取
     */
    TimeZone getTimeZone();
}
