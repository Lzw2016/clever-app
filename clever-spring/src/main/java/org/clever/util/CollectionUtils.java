package org.clever.util;

import java.util.*;

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

    /**
     * 返回指定多值Map的不可修改视图
     *
     * @param targetMap 返回不可修改视图的Map
     * @return 指定多值Map的不可修改视图
     */
    @SuppressWarnings("unchecked")
    public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(MultiValueMap<? extends K, ? extends V> targetMap) {
        Assert.notNull(targetMap, "'targetMap' must not be null");
        Map<K, List<V>> result = newLinkedHashMap(targetMap.size());
        targetMap.forEach((key, value) -> {
            List<? extends V> values = Collections.unmodifiableList(value);
            result.put(key, (List<V>) values);
        });
        Map<K, List<V>> unmodifiableMap = Collections.unmodifiableMap(result);
        return toMultiValueMap(unmodifiableMap);
    }

    /**
     * 将 {@code Map<K, List<V>>} 调整为 {@code MultiValueMap<K, V>}.
     *
     * @param targetMap 原始Map
     * @return 调整后的多值Map（包装原始Map）
     */
    public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
        return new MultiValueMapAdapter<>(targetMap);
    }

    /**
     * 检索给定列表的第一个元素，访问零索引
     *
     * @param list 要检查的列表（可以是 {@code null} 或为空）
     * @return 第一个元素，如果没有则为 {@code null}
     */
    public static <T> T firstElement(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }
}
