package org.clever.core;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/30 20:03 <br/>
 */
public class NamingUtils {
    public static final Pattern LOWER_CHAR_PATTERN = Pattern.compile("[a-z]+");
    public static final char UNDERLINE = '_';

    // --------------------------------------------------------------------------------------------
    // String
    // --------------------------------------------------------------------------------------------

    /**
     * 驼峰 转 下划线(全小写)
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
     * 下划线 转 驼峰(小写驼峰)
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

    // --------------------------------------------------------------------------------------------
    // Map
    // --------------------------------------------------------------------------------------------

    /**
     * 下划线 转 驼峰 (映射缓存)
     *
     * @param dataMap        数据
     * @param mapping        映射缓存(根据 dataMap 的 key 进行填充)
     * @param renameStrategy key默认的重名了策略
     */
    private static void fillMapping(Map<String, Object> dataMap, Map<String, String> mapping, RenameStrategy renameStrategy) {
        if (mapping == null || dataMap == null) {
            return;
        }
        for (String key : dataMap.keySet()) {
            if (mapping.containsKey(key)) {
                continue;
            }
            mapping.put(key, rename(key, renameStrategy));
        }
    }

    /**
     * Map key 重命名
     *
     * @param dataMap        数据
     * @param mapping        映射缓存(根据 dataMap 的 key 进行填充)
     * @param renameStrategy key默认的重名了策略
     * @return key 重命名后的 Map
     */
    public static Map<String, Object> renameMapKey(Map<String, Object> dataMap, Map<String, String> mapping, RenameStrategy renameStrategy) {
        if (dataMap == null) {
            return null;
        }
        if (mapping == null) {
            mapping = new HashMap<>(dataMap.size());
        }
        fillMapping(dataMap, mapping, renameStrategy);
        Map<String, Object> result = new LinkedHashMap<>(dataMap.size());
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            key = mapping.getOrDefault(key, key);
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * Map key 下划线 转 驼峰(小写驼峰)
     *
     * @param dataMap 数据
     * @param mapping key重命名映射配置
     */
    public static Map<String, Object> underlineToCamel(Map<String, Object> dataMap, Map<String, String> mapping) {
        return renameMapKey(dataMap, mapping, RenameStrategy.ToCamel);
    }

    /**
     * key下划线转驼峰格式
     *
     * @param dataMap 数据
     */
    public static Map<String, Object> underlineToCamel(Map<String, Object> dataMap) {
        return renameMapKey(dataMap, null, RenameStrategy.ToCamel);
    }

    /**
     * Map key 驼峰 转 下划线(全小写)
     *
     * @param dataMap 数据
     * @param mapping key重命名映射配置
     */
    public static Map<String, Object> camelToUnderline(Map<String, Object> dataMap, Map<String, String> mapping) {
        return renameMapKey(dataMap, mapping, RenameStrategy.ToUnderline);
    }

    /**
     * Map key 驼峰 转 下划线(全小写)
     *
     * @param dataMap 数据
     */
    public static Map<String, Object> camelToUnderline(Map<String, Object> dataMap) {
        return renameMapKey(dataMap, null, RenameStrategy.ToUnderline);
    }
}
