package org.clever.context.i18n;

import java.util.Locale;

/**
 * 用于获取当前环境Locale对象的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 17:01 <br/>
 */
public interface LocaleContext {
    /**
     * 返回当前的Locale，可以是固定的，也可以动态获取
     */
    Locale getLocale();
}