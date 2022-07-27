package org.clever.core;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/30 20:03 <br/>
 */
public class NamingUtils {
    private static final Pattern LOWER_CHAR_PATTERN = Pattern.compile("[a-z]+");
    private static final Pattern CAMEL_PATTERN = Pattern.compile("[A-Z]([a-z\\d]+)?");

    private static final Pattern UNDERLINE_TO_CAMEL_PATTERN = Pattern.compile("([A-Za-z\\d]+)(_)?");
    private static final Pattern CAMEL_TO_UNDERLINE_PATTERN = CAMEL_PATTERN;

    private static final Pattern MIDDLE_LINE_TO_CAMEL_PATTERN = Pattern.compile("([A-Za-z\\d]+)(-)?");
    private static final Pattern CAMEL_TO_MIDDLE_LINE_PATTERN = CAMEL_PATTERN;

    public static final char UNDERLINE = '_';

    /**
     * 下划线转驼峰法
     *
     * @param line 源字符串
     * @return 转换后的字符串
     */
    public static String underline2Camel(String line) {
        return toCamel(line, true, '_', UNDERLINE_TO_CAMEL_PATTERN);
    }

    /**
     * 下划线转驼峰法
     *
     * @param line       源字符串
     * @param smallCamel 大小驼峰,是否为小驼峰
     * @return 转换后的字符串
     */
    public static String underline2Camel(String line, boolean smallCamel) {
        return toCamel(line, smallCamel, '_', UNDERLINE_TO_CAMEL_PATTERN);
    }

    /**
     * 驼峰法转下划线
     *
     * @param line 源字符串
     * @return 转换后的字符串
     */
    public static String camel2Underline(String line) {
        return camelTo(line, false, CAMEL_TO_UNDERLINE_PATTERN, '_');
    }

    /**
     * 驼峰法转下划线
     *
     * @param line      源字符串
     * @param upperCase 是否大写
     * @return 转换后的字符串
     */
    public static String camel2Underline(String line, boolean upperCase) {
        return camelTo(line, upperCase, CAMEL_TO_UNDERLINE_PATTERN, '_');
    }

    /**
     * 中划线转驼峰法
     *
     * @param line 源字符串
     * @return 转换后的字符串
     */
    public static String middleLine2Camel(String line) {
        return toCamel(line, true, '-', MIDDLE_LINE_TO_CAMEL_PATTERN);
    }

    /**
     * 中划线转驼峰法
     *
     * @param line       源字符串
     * @param smallCamel 大小驼峰,是否为小驼峰
     * @return 转换后的字符串
     */
    public static String middleLine2Camel(String line, boolean smallCamel) {
        return toCamel(line, smallCamel, '-', MIDDLE_LINE_TO_CAMEL_PATTERN);
    }

    /**
     * 驼峰法转中划线
     *
     * @param line 源字符串
     * @return 转换后的字符串
     */
    public static String camel2MiddleLine(String line) {
        return camelTo(line, false, CAMEL_TO_MIDDLE_LINE_PATTERN, '-');
    }

    /**
     * 驼峰法转中划线
     *
     * @param line      源字符串
     * @param upperCase 是否大写
     * @return 转换后的字符串
     */
    public static String camel2MiddleLine(String line, boolean upperCase) {
        return camelTo(line, upperCase, CAMEL_TO_MIDDLE_LINE_PATTERN, '-');
    }

    /**
     * ?转驼峰法
     *
     * @param line       源字符串
     * @param smallCamel 大小驼峰,是否为小驼峰
     * @param split      源字符串分割字符
     * @param pattern    源字符串匹配模式
     * @return 转换后的字符串
     */
    private static String toCamel(String line, boolean smallCamel, char split, Pattern pattern) {
        if (StringUtils.isEmpty(line)) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String word = matcher.group();
            sb.append(smallCamel && matcher.start() == 0 ? Character.toLowerCase(word.charAt(0)) : Character.toUpperCase(word.charAt(0)));
            int index = word.lastIndexOf(split);
            if (index > 0) {
                sb.append(word.substring(1, index).toLowerCase());
            } else {
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * 驼峰法转?
     *
     * @param line      源字符串
     * @param upperCase 是否大写
     * @param pattern   源字符串匹配模式
     * @param split     新字符串的分隔符
     * @return 转换后的字符串
     */
    private static String camelTo(String line, boolean upperCase, Pattern pattern, char split) {
        if (StringUtils.isEmpty(line)) {
            return StringUtils.EMPTY;
        }
        line = String.valueOf(line.charAt(0)).toUpperCase().concat(line.substring(1));
        StringBuilder sb = new StringBuilder();
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String word = matcher.group();
            sb.append(upperCase ? word.toUpperCase() : word.toLowerCase());
            sb.append(matcher.end() == line.length() ? StringUtils.EMPTY : split);
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 字符串驼峰转下划线格式(全小写)
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String camelToUnderline(String param) {
        if (StringUtils.isBlank(param)) {
            return StringUtils.EMPTY;
        }
        // 没有小写字母时(全是“大写字母”和“_”)
        Matcher matcher = LOWER_CHAR_PATTERN.matcher(param);
        if (!matcher.find()) {
            return StringUtils.lowerCase(param);
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len + 16);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append(UNDERLINE);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 字符串下划线转驼峰格式(小写驼峰)
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String underlineToCamel(String param) {
        if (StringUtils.isBlank(param)) {
            return StringUtils.EMPTY;
        }
        int len = param.length();
        param = param.toLowerCase();
        StringBuilder sb = new StringBuilder(len);
        boolean flag = false;
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Objects.equals(UNDERLINE, c)) {
                flag = true;
                continue;
            }
            if (flag) {
                flag = false;
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰格式映射缓存
     *
     * @param mapping 映射缓存
     * @param dataMap 数据
     */
    private static void underlineToCamelMapping(Map<String, String> mapping, Map<String, Object> dataMap) {
        if (mapping == null || dataMap == null) {
            return;
        }
        for (String key : dataMap.keySet()) {
            if (mapping.containsKey(key)) {
                continue;
            }
            mapping.put(key, underlineToCamel(key));
        }
    }

    /**
     * key下划线转驼峰格式
     *
     * @param dataMap 数据
     * @param mapping key重命名映射配置
     */
    public static Map<String, Object> mapKeyRename(Map<String, Object> dataMap, Map<String, String> mapping) {
        if (dataMap == null) {
            return null;
        }
        if (mapping == null || mapping.isEmpty()) {
            mapping = new HashMap<>(dataMap.size());
            underlineToCamelMapping(mapping, dataMap);
        }
        Map<String, Object> result = new LinkedHashMap<>(dataMap.size());
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = mapping.getOrDefault(entry.getKey(), entry.getKey());
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * key下划线转驼峰格式
     *
     * @param dataMap 数据
     */
    public static Map<String, Object> underlineToCamel(Map<String, Object> dataMap) {
        return mapKeyRename(dataMap, null);
    }

    /**
     * map集合的key重命名
     *
     * @param dataMapList 数据集合
     * @param mapping     key重命名映射配置
     */
    public static List<Map<String, Object>> mapKeyRename(List<Map<String, Object>> dataMapList, Map<String, String> mapping) {
        if (dataMapList == null) {
            return null;
        }
        boolean mappingIsNull = false;
        List<Map<String, Object>> result = new ArrayList<>(dataMapList.size());
        for (Map<String, Object> map : dataMapList) {
            if (mapping == null || mapping.isEmpty()) {
                mapping = new HashMap<>(map.size());
                mappingIsNull = true;
            }
            if (mappingIsNull) {
                underlineToCamelMapping(mapping, map);
            }
            result.add(mapKeyRename(map, mapping));
        }
        return result;
    }

    /**
     * key下划线转驼峰格式
     *
     * @param dataMapList 数据集合
     */
    public static List<Map<String, Object>> underlineToCamel(List<Map<String, Object>> dataMapList) {
        return mapKeyRename(dataMapList, null);
    }

    /**
     * 重命名
     *
     * @param name           原名称
     * @param renameStrategy 重命名策略
     */
    public static String rename(String name, RenameStrategy renameStrategy) {
        if (renameStrategy == null) {
            return name;
        }
        switch (renameStrategy) {
            case ToCamel:
                // 小写驼峰
                name = underlineToCamel(name);
                break;
            case ToUnderline:
                // 全小写下划线
                name = camelToUnderline(name);
                break;
            case ToUpperCase:
                // 全大写
                name = StringUtils.upperCase(name);
                break;
            case ToLowerCase:
                // 全小写
                name = StringUtils.lowerCase(name);
                break;
        }
        return name;
    }
}
