package org.clever.util;

import java.io.Serializable;
import java.util.*;

/**
 * {@link MultiValueMap}适配器，适配{@link Map}的api
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 15:07 <br/>
 */
public class MultiValueMapAdapter<K, V> implements MultiValueMap<K, V>, Serializable {
    private final Map<K, List<V>> targetMap;

    /**
     * Wrap the given target {@link Map} as a {@link MultiValueMap} adapter.
     *
     * @param targetMap the plain target {@code Map}
     */
    public MultiValueMapAdapter(Map<K, List<V>> targetMap) {
        Assert.notNull(targetMap, "'targetMap' must not be null");
        this.targetMap = targetMap;
    }

    // MultiValueMap implementation

    @Override
    public V getFirst(K key) {
        List<V> values = this.targetMap.get(key);
        return (values != null && !values.isEmpty() ? values.get(0) : null);
    }

    @Override
    public void add(K key, V value) {
        List<V> values = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
        values.add(value);
    }

    @Override
    public void addAll(K key, List<? extends V> values) {
        List<V> currentValues = this.targetMap.computeIfAbsent(key, k -> new ArrayList<>(1));
        currentValues.addAll(values);
    }

    @Override
    public void addAll(MultiValueMap<K, V> values) {
        for (Entry<K, List<V>> entry : values.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void set(K key, V value) {
        List<V> values = new ArrayList<>(1);
        values.add(value);
        this.targetMap.put(key, values);
    }

    @Override
    public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    @Override
    public Map<K, V> toSingleValueMap() {
        Map<K, V> singleValueMap = CollectionUtils.newLinkedHashMap(this.targetMap.size());
        this.targetMap.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                singleValueMap.put(key, values.get(0));
            }
        });
        return singleValueMap;
    }

    // Map implementation

    @Override
    public int size() {
        return this.targetMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.targetMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.targetMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.targetMap.containsValue(value);
    }

    @Override
    public List<V> get(Object key) {
        return this.targetMap.get(key);
    }

    @Override
    public List<V> put(K key, List<V> value) {
        return this.targetMap.put(key, value);
    }

    @Override
    public List<V> remove(Object key) {
        return this.targetMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> map) {
        this.targetMap.putAll(map);
    }

    @Override
    public void clear() {
        this.targetMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.targetMap.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return this.targetMap.values();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return this.targetMap.entrySet();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object other) {
        return (this == other || this.targetMap.equals(other));
    }

    @Override
    public int hashCode() {
        return this.targetMap.hashCode();
    }

    @Override
    public String toString() {
        return this.targetMap.toString();
    }
}

