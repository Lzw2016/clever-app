package org.clever.core.env;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * {@link PropertySources}接口的默认实现。
 * 允许操纵包含的属性源，并提供用于复制现有{@code PropertySources}实例的构造函数。
 * 在{@link #addFirst}和{@link #addLast}等方法中提到优先级时，
 * 这与使用{@link PropertyResolver}解析给定属性时搜索属性源的顺序有关
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:20 <br/>
 *
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {
    private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();

    /**
     * 新建 {@link MutablePropertySources}
     */
    public MutablePropertySources() {
    }

    /**
     * 从给定的propertySources对象创建新的{@code MutablePropertySources}，保留包含的{@code PropertySource}对象的原始顺序
     */
    public MutablePropertySources(PropertySources propertySources) {
        this();
        for (PropertySource<?> propertySource : propertySources) {
            addLast(propertySource);
        }
    }

    @Override
    public Iterator<PropertySource<?>> iterator() {
        return this.propertySourceList.iterator();
    }

    @Override
    public Spliterator<PropertySource<?>> spliterator() {
        return Spliterators.spliterator(this.propertySourceList, 0);
    }

    @Override
    public Stream<PropertySource<?>> stream() {
        return this.propertySourceList.stream();
    }

    @Override
    public boolean contains(String name) {
        for (PropertySource<?> propertySource : this.propertySourceList) {
            if (propertySource.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PropertySource<?> get(String name) {
        for (PropertySource<?> propertySource : this.propertySourceList) {
            if (propertySource.getName().equals(name)) {
                return propertySource;
            }
        }
        return null;
    }

    /**
     * 添加具有最高优先级的给定属性源对象。
     */
    public void addFirst(PropertySource<?> propertySource) {
        synchronized (this.propertySourceList) {
            removeIfPresent(propertySource);
            this.propertySourceList.add(0, propertySource);
        }
    }

    /**
     * 添加具有最低优先级的给定属性源对象。
     */
    public void addLast(PropertySource<?> propertySource) {
        synchronized (this.propertySourceList) {
            removeIfPresent(propertySource);
            this.propertySourceList.add(propertySource);
        }
    }

    /**
     * 添加给定的属性源对象，其优先级立即高于命名的相对属性源
     */
    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        synchronized (this.propertySourceList) {
            removeIfPresent(propertySource);
            int index = assertPresentAndGetIndex(relativePropertySourceName);
            addAtIndex(index, propertySource);
        }
    }

    /**
     * 添加给定的属性源对象，其优先级立即低于命名的相对属性源
     */
    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        synchronized (this.propertySourceList) {
            removeIfPresent(propertySource);
            int index = assertPresentAndGetIndex(relativePropertySourceName);
            addAtIndex(index + 1, propertySource);
        }
    }

    /**
     * 返回给定属性源的优先级，如果未找到返回 {@code -1}
     */
    public int precedenceOf(PropertySource<?> propertySource) {
        return this.propertySourceList.indexOf(propertySource);
    }

    /**
     * 删除并返回具有给定名称的属性源，如果找不到，则返回null
     *
     * @param name 要查找和删除的属性源的名称
     */
    public PropertySource<?> remove(String name) {
        synchronized (this.propertySourceList) {
            int index = this.propertySourceList.indexOf(PropertySource.named(name));
            return (index != -1 ? this.propertySourceList.remove(index) : null);
        }
    }

    /**
     * 用给定的属性源对象替换具有给定名称的属性源
     *
     * @param name           要查找和替换的属性源的名称
     * @param propertySource 替换属性源
     * @throws IllegalArgumentException 如果不存在具有给定名称的属性源
     * @see #contains
     */
    public void replace(String name, PropertySource<?> propertySource) {
        synchronized (this.propertySourceList) {
            int index = assertPresentAndGetIndex(name);
            this.propertySourceList.set(index, propertySource);
        }
    }

    /**
     * 返回包含的PropertySource对象数
     */
    public int size() {
        return this.propertySourceList.size();
    }

    @Override
    public String toString() {
        return this.propertySourceList.toString();
    }

    /**
     * 确保未相对于自身添加给定的属性源
     */
    protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
        String newPropertySourceName = propertySource.getName();
        if (relativePropertySourceName.equals(newPropertySourceName)) {
            throw new IllegalArgumentException("PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
        }
    }

    /**
     * 删除给定的属性源（如果存在）
     */
    protected void removeIfPresent(PropertySource<?> propertySource) {
        this.propertySourceList.remove(propertySource);
    }

    /**
     * 在列表中的特定索引处添加给定的属性源
     */
    private void addAtIndex(int index, PropertySource<?> propertySource) {
        removeIfPresent(propertySource);
        this.propertySourceList.add(index, propertySource);
    }

    /**
     * 断言命名属性源存在并返回其索引
     *
     * @param name 要查找的{@linkplain PropertySource#getName() 属性源的名称}
     * @throws IllegalArgumentException 如果命名属性源不存在
     */
    private int assertPresentAndGetIndex(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        if (index == -1) {
            throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
        }
        return index;
    }
}
