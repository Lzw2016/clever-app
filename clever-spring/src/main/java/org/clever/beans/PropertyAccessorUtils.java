package org.clever.beans;

/**
 * 根据{@link PropertyAccessor}接口执行bean属性访问的类的实用方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:10 <br/>
 */
public abstract class PropertyAccessorUtils {
    /**
     * 返回给定属性路径的实际属性名称
     *
     * @param propertyPath 用于确定属性名称的属性路径(可以包括属性键，例如用于指定映射条目的属性键)
     * @return 实际属性名称，不含任何关键元素
     */
    public static String getPropertyName(String propertyPath) {
        int separatorIndex = (propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX) ? propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1);
        return (separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath);
    }

    /**
     * 确定给定的注册路径是否与给定的属性路径匹配，指示属性本身或属性的索引元素
     *
     * @param propertyPath   属性路径
     * @param registeredPath 注册的路径
     * @return 路径是否匹配
     */
    public static boolean matchesProperty(String registeredPath, String propertyPath) {
        if (!registeredPath.startsWith(propertyPath)) {
            return false;
        }
        if (registeredPath.length() == propertyPath.length()) {
            return true;
        }
        if (registeredPath.charAt(propertyPath.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
            return false;
        }
        return (registeredPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, propertyPath.length() + 1) == registeredPath.length() - 1);
    }

    /**
     * 确定给定属性路径的规范名称。从映射键中删除周围引号：<br>
     * {@code map['key']} &rarr; {@code map[key]}<br>
     * {@code map["key"]} &rarr; {@code map[key]}
     *
     * @param propertyName bean属性路径
     * @return 属性路径的规范表示
     */
    public static String canonicalPropertyName(String propertyName) {
        if (propertyName == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(propertyName);
        int searchIndex = 0;
        while (searchIndex != -1) {
            int keyStart = sb.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX, searchIndex);
            searchIndex = -1;
            if (keyStart != -1) {
                int keyEnd = sb.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX, keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length());
                if (keyEnd != -1) {
                    String key = sb.substring(keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length(), keyEnd);
                    if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
                        sb.delete(keyStart + 1, keyStart + 2);
                        sb.delete(keyEnd - 2, keyEnd - 1);
                        keyEnd = keyEnd - 2;
                    }
                    searchIndex = keyEnd + PropertyAccessor.PROPERTY_KEY_SUFFIX.length();
                }
            }
        }
        return sb.toString();
    }

    /**
     * 确定给定属性路径的规范名称。
     *
     * @param propertyNames bean属性路径（作为数组）
     * @return 属性路径的规范表示（作为相同大小的数组）
     * @see #canonicalPropertyName(String)
     */
    public static String[] canonicalPropertyNames(String[] propertyNames) {
        if (propertyNames == null) {
            return null;
        }
        String[] result = new String[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            result[i] = canonicalPropertyName(propertyNames[i]);
        }
        return result;
    }

    /**
     * 确定给定属性路径中的第一个嵌套属性分隔符，忽略键中的点(如"map[my.key]")
     *
     * @param propertyPath 要检查的属性路径
     * @return 嵌套属性分隔符的索引，如果没有，则为-1
     */
    public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
        return getNestedPropertySeparatorIndex(propertyPath, false);
    }

    /**
     * 确定给定属性路径中的第一个嵌套属性分隔符，忽略键中的点(如"map[my.key]")
     *
     * @param propertyPath 要检查的属性路径
     * @return 嵌套属性分隔符的索引，如果没有，则为-1
     */
    public static int getLastNestedPropertySeparatorIndex(String propertyPath) {
        return getNestedPropertySeparatorIndex(propertyPath, true);
    }

    /**
     * 确定给定属性路径中的第一个(或最后一个)嵌套属性分隔符，忽略键中的点(如"map[my.key]")
     *
     * @param propertyPath 要检查的属性路径
     * @param last         是否返回最后一个分隔符而不是第一个分隔符
     * @return 嵌套属性分隔符的索引，如果没有，则为-1
     */
    private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
        boolean inKey = false;
        int length = propertyPath.length();
        int i = (last ? length - 1 : 0);
        while (last ? i >= 0 : i < length) {
            switch (propertyPath.charAt(i)) {
                case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
                case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
                    inKey = !inKey;
                    break;
                case PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR:
                    if (!inKey) {
                        return i;
                    }
            }
            if (last) {
                i--;
            } else {
                i++;
            }
        }
        return -1;
    }
}
