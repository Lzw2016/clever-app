package org.clever.util;

/**
 * 简单模式匹配的实用方法，特别是典型的"xxx*", "*xxx"和"*xxx*"模式样式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:17 <br/>
 */
public abstract class PatternMatchUtils {
    /**
     * 根据给定模式匹配字符串，支持以下简单模式样式："xxx*", "*xxx", "*xxx*" 和 "xxx*yyy"匹配（具有任意数量的模式部分）以及直接相等。
     *
     * @param pattern 要匹配的模式
     * @param str     要匹配的字符串
     * @return 字符串是否与给定的模式匹配
     */
    public static boolean simpleMatch(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }
        int firstIndex = pattern.indexOf('*');
        if (firstIndex == -1) {
            return pattern.equals(str);
        }
        if (firstIndex == 0) {
            if (pattern.length() == 1) {
                return true;
            }
            int nextIndex = pattern.indexOf('*', 1);
            if (nextIndex == -1) {
                return str.endsWith(pattern.substring(1));
            }
            String part = pattern.substring(1, nextIndex);
            if (part.isEmpty()) {
                return simpleMatch(pattern.substring(nextIndex), str);
            }
            int partIndex = str.indexOf(part);
            while (partIndex != -1) {
                if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
                    return true;
                }
                partIndex = str.indexOf(part, partIndex + 1);
            }
            return false;
        }
        return (str.length() >= firstIndex
                && pattern.startsWith(str.substring(0, firstIndex))
                && simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
    }

    /**
     * 根据给定的模式匹配字符串，支持以下简单的模式样式："xxx*", "*xxx", "*xxx*" 和 "xxx*yyy"匹配（具有任意数量的模式部分）以及直接相等。
     *
     * @param patterns 要匹配的模式
     * @param str      要匹配的字符串
     * @return 字符串是否与任何给定模式匹配
     */
    public static boolean simpleMatch(String[] patterns, String str) {
        if (patterns != null) {
            for (String pattern : patterns) {
                if (simpleMatch(pattern, str)) {
                    return true;
                }
            }
        }
        return false;
    }
}
