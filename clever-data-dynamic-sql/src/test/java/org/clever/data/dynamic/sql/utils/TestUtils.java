package org.clever.data.dynamic.sql.utils;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 10:24 <br/>
 */
public class TestUtils {
    /**
     * 删除多余的空白字符
     */
    public static String deleteWhitespace(String sql) {
        if (sql == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(sql.length());
        boolean preIsWhitespace = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            boolean isWhitespace = Character.isWhitespace(ch);
            if (preIsWhitespace) {
                // 之前是空白字符
                if (isWhitespace) {
                    // 当前是空白字符
                    continue;
                } else {
                    // 当前非空白字符
                    sb.append(ch);
                }
            } else {
                // 之前非空白字符
                if (isWhitespace) {
                    // 当前是空白字符
                    sb.append(' ');
                } else {
                    // 当前非空白字符
                    sb.append(ch);
                }
            }
            preIsWhitespace = isWhitespace;
        }
        return sb.toString();
    }
}
