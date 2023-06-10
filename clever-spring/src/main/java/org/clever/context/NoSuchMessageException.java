package org.clever.context;

import java.util.Locale;

/**
 * 无法解析消息时抛出异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/10 21:11 <br/>
 */
public class NoSuchMessageException extends RuntimeException {
    /**
     * @param code   无法为给定语言环境解析的代码
     * @param locale 用于在其中搜索代码的语言环境
     */
    public NoSuchMessageException(String code, Locale locale) {
        super("No message found under code '" + code + "' for locale '" + locale + "'.");
    }

    /**
     * @param code 无法为给定语言环境解析的代码
     */
    public NoSuchMessageException(String code) {
        super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
    }
}
