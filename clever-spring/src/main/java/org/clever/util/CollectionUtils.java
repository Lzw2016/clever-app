package org.clever.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 集合工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:58 <br/>
 */
public class CollectionUtils {
    /**
     * {@link HashMap}/{@link LinkedHashMap} 的默认的loadFactor值
     *
     * @see #newHashMap(int)
     * @see #newLinkedHashMap(int)
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 如果提供的Map为null或为空，则返回true。否则，返回false。
     *
     * @param map 要检查的Map
     * @return 给定的Map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    /**
     * 集合是否为空或者null
     */
    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
        return new HashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
        return new LinkedHashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
    }

    /**
     * 确定给定集合是否仅包含单个唯一对象
     *
     * @param collection 要检查的集合
     * @return 如果集合包含对同一实例的单个引用或多个引用，则为true，否则为false
     */
    public static boolean hasUniqueObject(Collection<?> collection) {
        if (isEmpty(collection)) {
            return false;
        }
        boolean hasCandidate = false;
        Object candidate = null;
        for (Object elem : collection) {
            if (!hasCandidate) {
                hasCandidate = true;
                candidate = elem;
            } else if (candidate != elem) {
                return false;
            }
        }
        return true;
    }
}
