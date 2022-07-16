package org.clever.format;

import java.text.ParseException;
import java.util.Locale;

/**
 * 解析文本字符串以生成T类型的实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:41 <br/>
 */
@FunctionalInterface
public interface Parser<T> {
    /**
     * 解析文本字符串以生成T类型的实例
     *
     * @param text   文本字符串
     * @param locale 当前用户Locale
     * @return T类型的实例
     * @throws ParseException           当在{@code java.text}库中发生解析异常时
     * @throws IllegalArgumentException 当发生解析异常时
     */
    T parse(String text, Locale locale) throws ParseException;
}
