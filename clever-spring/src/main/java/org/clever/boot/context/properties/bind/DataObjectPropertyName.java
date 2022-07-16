package org.clever.boot.context.properties.bind;

/**
 * 处理数据对象属性名称时提供帮助的内部实用程序。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:10 <br/>
 *
 * @see DataObjectBinder
 */
public abstract class DataObjectPropertyName {
    private DataObjectPropertyName() {
    }

    /**
     * 以虚线形式返回指定的Java Bean属性名称。
     *
     * @param name 源名称
     * @return 来自的虚线
     */
    public static String toDashedForm(String name) {
        StringBuilder result = new StringBuilder(name.length());
        boolean inIndex = false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (inIndex) {
                result.append(ch);
                if (ch == ']') {
                    inIndex = false;
                }
            } else {
                if (ch == '[') {
                    inIndex = true;
                    result.append(ch);
                } else {
                    ch = (ch != '_') ? ch : '-';
                    if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '-') {
                        result.append('-');
                    }
                    result.append(Character.toLowerCase(ch));
                }
            }
        }
        return result.toString();
    }
}
