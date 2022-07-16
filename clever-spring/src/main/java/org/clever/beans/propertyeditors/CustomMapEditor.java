package org.clever.beans.propertyeditors;

import org.clever.util.Assert;
import org.clever.util.ReflectionUtils;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Map 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:42 <br/>
 *
 * @see java.util.Map
 * @see java.util.SortedMap
 */
public class CustomMapEditor extends PropertyEditorSupport {
    @SuppressWarnings("rawtypes")
    private final Class<? extends Map> mapType;
    private final boolean nullAsEmptyMap;

    /**
     * @param mapType 目标类型，需要是Map的子接口或具体的Map类
     * @see java.util.Map
     * @see java.util.HashMap
     * @see java.util.TreeMap
     * @see java.util.LinkedHashMap
     */
    @SuppressWarnings("rawtypes")
    public CustomMapEditor(Class<? extends Map> mapType) {
        this(mapType, false);
    }

    /**
     * @param mapType        目标类型，需要是Map的子接口或具体的Map类
     * @param nullAsEmptyMap 是否将传入的null值转换为空映射（适当类型）
     * @see java.util.Map
     * @see java.util.TreeMap
     * @see java.util.LinkedHashMap
     */
    @SuppressWarnings("rawtypes")
    public CustomMapEditor(Class<? extends Map> mapType, boolean nullAsEmptyMap) {
        Assert.notNull(mapType, "Map type is required");
        if (!Map.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Map type [" + mapType.getName() + "] does not implement [java.util.Map]");
        }
        this.mapType = mapType;
        this.nullAsEmptyMap = nullAsEmptyMap;
    }

    /**
     * 将给定的文本值转换为具有单个元素的Map
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(text);
    }

    /**
     * 将给定值转换为目标类型的Map
     */
    @Override
    public void setValue(Object value) {
        if (value == null && this.nullAsEmptyMap) {
            super.setValue(createMap(this.mapType, 0));
        } else if (value == null || (this.mapType.isInstance(value) && !alwaysCreateNewMap())) {
            // Use the source value as-is, as it matches the target type.
            super.setValue(value);
        } else if (value instanceof Map) {
            // Convert Map elements.
            Map<?, ?> source = (Map<?, ?>) value;
            Map<Object, Object> target = createMap(this.mapType, source.size());
            source.forEach((key, val) -> target.put(convertKey(key), convertValue(val)));
            super.setValue(target);
        } else {
            throw new IllegalArgumentException("Value cannot be converted to Map: " + value);
        }
    }

    /**
     * 创建具有给定初始容量的给定类型的Map（如果Map类型支持）
     *
     * @param mapType         Map的子接口
     * @param initialCapacity 初始容量
     * @return 新Map实例
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map<Object, Object> createMap(Class<? extends Map> mapType, int initialCapacity) {
        if (!mapType.isInterface()) {
            try {
                return ReflectionUtils.accessibleConstructor(mapType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate map class: " + mapType.getName(), ex);
            }
        } else if (SortedMap.class == mapType) {
            return new TreeMap<>();
        } else {
            return new LinkedHashMap<>(initialCapacity);
        }
    }

    /**
     * 返回是否始终创建新Map，即使传入Map的类型已经匹配。
     * 默认为“false”；可以重写以强制创建新Map，例如在任何情况下转换元素
     *
     * @see #convertKey
     * @see #convertValue
     */
    protected boolean alwaysCreateNewMap() {
        return false;
    }

    /**
     * 转换Map key值
     *
     * @param key 源key
     * @return 要在目标Map中使用的key
     * @see #alwaysCreateNewMap
     */
    protected Object convertKey(Object key) {
        return key;
    }

    /**
     * 转换Map value值
     *
     * @param value 源值
     * @return 要在目标Map中使用的值
     * @see #alwaysCreateNewMap
     */
    protected Object convertValue(Object value) {
        return value;
    }

    /**
     * 此实现返回null，表示没有适当的文本表示
     */
    @Override
    public String getAsText() {
        return null;
    }
}
