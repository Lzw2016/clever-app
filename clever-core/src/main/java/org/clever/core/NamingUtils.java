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
    public static final Pattern LOWER_CHAR_PATTERN = Pattern.compile("[a-z]+");
    public static final char UNDERLINE = '_';

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
    public static List<Map<String, Object>> mapKeyRename(List<? extends Map<String, Object>> dataMapList, Map<String, String> mapping) {
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
    public static List<Map<String, Object>> underlineToCamel(List<? extends Map<String, Object>> dataMapList) {
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
