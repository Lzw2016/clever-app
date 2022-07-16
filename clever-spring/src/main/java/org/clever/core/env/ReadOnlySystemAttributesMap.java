package org.clever.core.env;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 由系统属性或环境变量支持的只读{@code Map<String, String>}实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:45 <br/>
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {
    @Override
    public boolean containsKey(Object key) {
        return (get(key) != null);
    }

    /**
     * 返回指定键映射到的值，如果此映射不包含键的映射，则返回null
     *
     * @param key 要检索的系统属性的名称
     * @throws IllegalArgumentException 如果给定键为非字符串
     */
    @Override
    public String get(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Type of key [" + key.getClass().getName() + "] must be java.lang.String");
        }
        return getSystemAttribute((String) key);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * 返回基础系统属性的模板方法。
     * <p>实现通常调用系统 {@link System#getProperty(String)} 或 {@link System#getenv(String)}
     */
    protected abstract String getSystemAttribute(String attributeName);

    // Unsupported

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> values() {
        return Collections.emptySet();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return Collections.emptySet();
    }
}
