package org.clever.util;

import java.util.List;
import java.util.Map;

/**
 * 一个key对应多个value的Map类型
 * <pre>{@code Map<K, List<V>>}</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:05 <br/>
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

    V getFirst(K key);

    void add(K key, V value);

    void addAll(K key, List<? extends V> values);

    void addAll(MultiValueMap<K, V> values);

    /**
     * 如果key不存在，则增加value
     */
    default void addIfAbsent(K key, V value) {
        if (!containsKey(key)) {
            add(key, value);
        }
    }

    /**
     * 为指定的key设置单个value
     */
    void set(K key, V value);

    void setAll(Map<K, V> values);

    Map<K, V> toSingleValueMap();
}
