package org.clever.data.dynamic.sql.parsing;

/**
 * Token处理器
 */
public interface TokenHandler {
    /**
     * 处理Token
     */
    String handleToken(String content);
}
