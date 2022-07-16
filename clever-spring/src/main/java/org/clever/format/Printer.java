package org.clever.format;

import java.util.Locale;

/**
 * 打印T类型的对象以供显示
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:40 <br/>
 */
@FunctionalInterface
public interface Printer<T> {
    /**
     * 打印类型为T的对象以供显示
     *
     * @param object 要打印的实例
     * @param locale 当前用户Locale
     * @return 打印的文本字符串
     */
    String print(T object, Locale locale);
}
