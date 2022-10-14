package org.clever.data.dynamic.sql.utils;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/24 19:14 <br/>
 */
public class SqlParameterNameStrategy {

    /**
     * sql参数名称重命名<br />
     * 替换 ( ) " ' ,
     */
    public static String rename(final String literal) {
        if (literal == null) {
            return "";
        }
        StringBuilder name = new StringBuilder(literal.length());
        for (int i = 0; i < literal.length(); i++) {
            char ch = literal.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            switch (ch) {
                case '(':
                case ')':
                case '"':
                case '\'':
                case ',':
                    name.append('_');
                    break;
                default:
                    name.append(ch);
            }
        }
        return name.toString();
    }

    // public static String uniqueName(String literal, Map<String, Object> paramMap)
}
