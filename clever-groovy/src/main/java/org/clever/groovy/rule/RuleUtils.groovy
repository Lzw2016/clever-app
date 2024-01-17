package org.clever.groovy.rule

import com.google.common.base.Strings
import org.clever.core.Conv

/**
 * 规则工具类
 *
 * @Author: yvan
 */
public class RuleUtils {

    /**
     * 根据一批实体类，找到实体类中的元数据，构建能用于前端UI的图形化条件构造面板
     */
    public static DomainMeta[] getDomainMeta(Object[] objects) {
        return new DomainMeta[0];
    }

    /**
     * 根据一批表名，构建能用于前端UI的图形化条件构造SQL片段的面板
     */
    public static DomainMeta[] getTableMeta(String[] tableNames) {
        return new DomainMeta[0];
    }


    /**
     * 安全转换字符串，吧字符串双引号变成\"，单引号变成\'
     */
    public static String safeString(Object valueRaw) {
        String value = Conv.asString(valueRaw)
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        return value.replace("\"", "\\\"").replace("'", "\\'");
    }

    /**
     * 安全转换字符串，并在两边加上适当的引号
     * 如果 valueRaw 里面包含回车键，就用三引号
     * 否则就用单引号
     */
    public static String safeStringWithComma(Object valueRaw) {
        String value = Conv.asString(valueRaw)
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        if (value.contains("\n")) {
            return "'''${value}'''";
        }
        return "\'${safeString(value)}\'";
    }

    /**
     * 按赋值等式的模式 ${name} = \"value\" 生成代码
     * 如果 value 不存在，就不插入代码
     */
    public static String safeCode(String name, String value) {
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        return "${name} = \"${safeString(value)}\"";
    }

    /**
     * 判断 key 字符串是否能安全的作为变量名，如果不能，就加上单引号
     */
    public static String safeKey(Object keyRaw) {
        String key = Conv.asString(keyRaw)
        if (Strings.isNullOrEmpty(key) || key == 'N/A') {
            return "";
        }
        if (!key.matches('^[a-zA-Z][a-zA-Z0-9_]*$')) {
            key = "\'" + safeString(key) + "\'";
        }
        return key
    }

    /**
     * 按赋值 Map 的模式生成代码
     * <pre>
     *     ${name} = [
     *          key1: value1,
     *          key2: value2,
     *          ...
     *     ]
     * </pre>
     * 如果 value == null 就不插入代码
     */
    public static String safeMap(String name, Map value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("${name} = [\n");
        for (Map.Entry entry : value.entrySet()) {
            String key = safeKey(entry.key);

            if (entry.value instanceof String) {
                sb.append("    ${key}: ${safeStringWithComma(entry.value)},\n");

            } else if (entry.value instanceof Long) {
                sb.append("    ${key}: ${safeString(entry.value)}L,\n");

            } else if (entry.value instanceof Map) {
                sb.append("    ${key}: [\n");
                for (Map.Entry entry2 : ((Map) entry.value).entrySet()) {
                    String innerKey = safeKey(entry2.key);
                    if (entry2.value instanceof String) {
                        sb.append("                ${innerKey}: \"${safeString(entry2.value)}\",\n");
                    } else if (entry2.value instanceof Long) {
                        sb.append("                ${innerKey}: ${safeString(entry2.value)}L,\n");
                    } else if (entry2.value instanceof Map) {
                        throw new RuntimeException('不支持两层 map 结构')
                    } else {
                        sb.append("                ${innerKey}: ${entry2.value},\n");
                    }
                }
                sb.append("    ],\n");

            } else {
                sb.append("        ${key}: ${entry.value},\n");
            }
        }
        sb.append("    ]");
        return sb.toString();
    }

    /**
     * 如果属性值 Long 不为空就插入 ${name} = valueL
     */
    public static String safeCode(String name, Long value) {
        if (value == null) {
            return "";
        }
        return "${name} = ${value}L";
    }

    /**
     * 安全转换方法体字符串
     */
    public static String safeMethod(String value) {
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        return value.stripIndent()
    }

    /**
     * when 不存在时，返回空字符串，否则返回 when 的代码
     */
    public static String safeWhen(String value) {
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        return """
        when {
            ${safeMethod(value)}
        }"""
    }

    /**
     * then 不存在时，返回空字符串，否则返回 then 的代码
     */
    public static String safeThen(String value) {
        if (Strings.isNullOrEmpty(value) || value == 'N/A') {
            return "";
        }
        return """
        then {
            ${safeMethod(value)}
        }"""
    }
}
