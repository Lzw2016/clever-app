package org.clever.util;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link LinkedHashMap} 以不区分大小写的方式存储字符串键的变体，例如用于结果表中基于键的访问。
 * <p>保留键的原始顺序和原始大小写，同时允许使用任何大小写的键进行包含、获取和移除调用。
 * <p>不支持{@code null} key
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:05 <br/>
 *
 * @param <V> the value type
 */
public class LinkedCaseInsensitiveMap<V> implements Map<String, V>, Serializable, Cloneable {
    private final LinkedHashMap<String, V> targetMap;
    private final HashMap<String, String> caseInsensitiveKeys;
    private final Locale locale;
    private transient volatile Set<String> keySet;
    private transient volatile Collection<V> values;
    private transient volatile Set<Entry<String, V>> entrySet;

    /**
     * 创建一个新的LinkedCaseInsensitiveMap，根据默认语言环境（默认情况下为小写）存储不区分大小写的键
     *
     * @see #convertKey(String)
     */
    public LinkedCaseInsensitiveMap() {
        this((Locale) null);
    }

    /**
     * 创建一个新的LinkedCaseInsensitiveMap，根据给定的语言环境（小写）存储不区分大小写的密钥。
     *
     * @param locale 用于不区分大小写的密钥转换的区域设置
     * @see #convertKey(String)
     */
    public LinkedCaseInsensitiveMap(Locale locale) {
        this(12, locale);  // 相当于LinkedHashMap的初始容量16
    }

    /**
     * 创建一个新的LinkedCaseInsensitiveMap，该映射将{@link LinkedHashMap}包装为初始容量，
     * 该容量可以容纳指定数量的元素，而无需进行任何预期的立即resize/rehash操作，
     * 并根据默认语言环境（小写）存储不区分大小写的密钥。
     *
     * @param expectedSize 预期的元素数量（导出相应的容量，以便不需要resize/rehash操作）
     * @see CollectionUtils#newHashMap(int)
     * @see #convertKey(String)
     */
    public LinkedCaseInsensitiveMap(int expectedSize) {
        this(expectedSize, null);
    }

    /**
     * 创建一个新的LinkedCaseInsensitiveMap，该映射将{@link LinkedHashMap}包装为初始容量，
     * 该容量可以容纳指定数量的元素，而无需进行任何预期的立即resize/rehash操作，
     * 并根据给定的区域设置存储不区分大小写的密钥（小写）。
     *
     * @param expectedSize 预期的元素数量（导出相应的容量，以便不需要resize/rehash操作）
     * @param locale       用于不区分大小写的密钥转换的区域设置
     * @see CollectionUtils#newHashMap(int)
     * @see #convertKey(String)
     */
    public LinkedCaseInsensitiveMap(int expectedSize, Locale locale) {
        this.targetMap = new LinkedHashMap<String, V>((int) (expectedSize / CollectionUtils.DEFAULT_LOAD_FACTOR), CollectionUtils.DEFAULT_LOAD_FACTOR) {
            @Override
            public boolean containsKey(Object key) {
                return LinkedCaseInsensitiveMap.this.containsKey(key);
            }

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                boolean doRemove = LinkedCaseInsensitiveMap.this.removeEldestEntry(eldest);
                if (doRemove) {
                    removeCaseInsensitiveKey(eldest.getKey());
                }
                return doRemove;
            }
        };
        this.caseInsensitiveKeys = CollectionUtils.newHashMap(expectedSize);
        this.locale = (locale != null ? locale : Locale.getDefault());
    }

    /**
     * 复制构造函数
     */
    @SuppressWarnings("unchecked")
    private LinkedCaseInsensitiveMap(LinkedCaseInsensitiveMap<V> other) {
        this.targetMap = (LinkedHashMap<String, V>) other.targetMap.clone();
        this.caseInsensitiveKeys = (HashMap<String, String>) other.caseInsensitiveKeys.clone();
        this.locale = other.locale;
    }

    // Implementation of java.util.Map

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
        return (key instanceof String && this.caseInsensitiveKeys.containsKey(convertKey((String) key)));
    }

    @Override
    public boolean containsValue(Object value) {
        return this.targetMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return this.targetMap.get(caseInsensitiveKey);
            }
        }
        return null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (key instanceof String) {
            String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return this.targetMap.get(caseInsensitiveKey);
            }
        }
        return defaultValue;
    }

    @Override
    public V put(String key, V value) {
        String oldKey = this.caseInsensitiveKeys.put(convertKey(key), key);
        V oldKeyValue = null;
        if (oldKey != null && !oldKey.equals(key)) {
            oldKeyValue = this.targetMap.remove(oldKey);
        }
        V oldValue = this.targetMap.put(key, value);
        return (oldKeyValue != null ? oldKeyValue : oldValue);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> map) {
        if (map.isEmpty()) {
            return;
        }
        map.forEach(this::put);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public V putIfAbsent(String key, V value) {
        String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            V oldKeyValue = this.targetMap.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return this.targetMap.putIfAbsent(key, value);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        String oldKey = this.caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            V oldKeyValue = this.targetMap.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return this.targetMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V remove(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = removeCaseInsensitiveKey((String) key);
            if (caseInsensitiveKey != null) {
                return this.targetMap.remove(caseInsensitiveKey);
            }
        }
        return null;
    }

    @Override
    public void clear() {
        this.caseInsensitiveKeys.clear();
        this.targetMap.clear();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = this.keySet;
        if (keySet == null) {
            keySet = new KeySet(this.targetMap.keySet());
            this.keySet = keySet;
        }
        return keySet;
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = this.values;
        if (values == null) {
            values = new Values(this.targetMap.values());
            this.values = values;
        }
        return values;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        Set<Entry<String, V>> entrySet = this.entrySet;
        if (entrySet == null) {
            entrySet = new EntrySet(this.targetMap.entrySet());
            this.entrySet = entrySet;
        }
        return entrySet;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public LinkedCaseInsensitiveMap<V> clone() {
        return new LinkedCaseInsensitiveMap<>(this);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
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

    // Specific to LinkedCaseInsensitiveMap

    /**
     * 返回此{@code LinkedCaseInsensitiveMap}使用的区域设置。
     * 用于不区分大小写的密钥转换。
     *
     * @see #LinkedCaseInsensitiveMap(Locale)
     * @see #convertKey(String)
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * 将给定密钥转换为不区分大小写的密钥。
     * <p>默认实现根据此映射的区域设置将键转换为小写。
     *
     * @param key 用户指定的密钥
     * @return 用于存储的密钥
     * @see String#toLowerCase(Locale)
     */
    protected String convertKey(String key) {
        return key.toLowerCase(getLocale());
    }

    /**
     * 确定此映射是否应删除给定的最旧条目。
     *
     * @param eldest 候选人条目
     * @return true表示删除，false表示保留
     * @see LinkedHashMap#removeEldestEntry
     */
    @SuppressWarnings("JavadocReference")
    protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
        return false;
    }

    private String removeCaseInsensitiveKey(String key) {
        return this.caseInsensitiveKeys.remove(convertKey(key));
    }

    private class KeySet extends AbstractSet<String> {
        private final Set<String> delegate;

        KeySet(Set<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<String> iterator() {
            return new KeySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            return LinkedCaseInsensitiveMap.this.remove(o) != null;
        }

        @Override
        public void clear() {
            LinkedCaseInsensitiveMap.this.clear();
        }

        @Override
        public Spliterator<String> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super String> action) {
            this.delegate.forEach(action);
        }
    }

    private class Values extends AbstractCollection<V> {
        private final Collection<V> delegate;

        Values(Collection<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<V> iterator() {
            return new ValuesIterator();
        }

        @Override
        public void clear() {
            LinkedCaseInsensitiveMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            this.delegate.forEach(action);
        }
    }

    private class EntrySet extends AbstractSet<Entry<String, V>> {
        private final Set<Entry<String, V>> delegate;

        public EntrySet(Set<Entry<String, V>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            if (this.delegate.remove(o)) {
                removeCaseInsensitiveKey(((Map.Entry<String, V>) o).getKey());
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.delegate.clear();
            caseInsensitiveKeys.clear();
        }

        @Override
        public Spliterator<Entry<String, V>> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super Entry<String, V>> action) {
            this.delegate.forEach(action);
        }
    }

    private abstract class EntryIterator<T> implements Iterator<T> {
        private final Iterator<Entry<String, V>> delegate;
        private Entry<String, V> last;

        public EntryIterator() {
            this.delegate = targetMap.entrySet().iterator();
        }

        protected Entry<String, V> nextEntry() {
            Entry<String, V> entry = this.delegate.next();
            this.last = entry;
            return entry;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public void remove() {
            this.delegate.remove();
            if (this.last != null) {
                removeCaseInsensitiveKey(this.last.getKey());
                this.last = null;
            }
        }
    }

    private class KeySetIterator extends EntryIterator<String> {
        @Override
        public String next() {
            return nextEntry().getKey();
        }
    }

    private class ValuesIterator extends EntryIterator<V> {
        @Override
        public V next() {
            return nextEntry().getValue();
        }
    }

    private class EntrySetIterator extends EntryIterator<Entry<String, V>> {
        @Override
        public Entry<String, V> next() {
            return nextEntry();
        }
    }
}
