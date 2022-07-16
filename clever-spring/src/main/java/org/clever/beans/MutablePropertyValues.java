package org.clever.beans;

import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * {@link PropertyValues}接口的默认实现。允许对属性进行简单操作，并提供构造函数来支持从映射进行深度复制和构造
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:23 <br/>
 */
public class MutablePropertyValues implements PropertyValues, Serializable {
    private final List<PropertyValue> propertyValueList;
    private Set<String> processedProperties;
    private volatile boolean converted;

    /**
     * 创建新的空MutablePropertyValue对象。可以使用add方法添加属性值
     *
     * @see #add(String, Object)
     */
    public MutablePropertyValues() {
        this.propertyValueList = new ArrayList<>(0);
    }

    /**
     * 深度复制构造函数。保证PropertyValue引用是独立的，尽管它不能深度复制单个PropertyValue对象当前引用的对象
     *
     * @param original 要复制的属性值
     * @see #addPropertyValues(PropertyValues)
     */
    public MutablePropertyValues(PropertyValues original) {
        // We can optimize this because it's all new: There is no replacement of existing property values.
        if (original != null) {
            PropertyValue[] pvs = original.getPropertyValues();
            this.propertyValueList = new ArrayList<>(pvs.length);
            for (PropertyValue pv : pvs) {
                this.propertyValueList.add(new PropertyValue(pv));
            }
        } else {
            this.propertyValueList = new ArrayList<>(0);
        }
    }

    /**
     * 从映射构造一个新的可变属性值对象
     *
     * @param original {@code Map<属性名, 属性值>}
     * @see #addPropertyValues(Map)
     */
    public MutablePropertyValues(Map<?, ?> original) {
        // We can optimize this because it's all new: There is no replacement of existing property values.
        if (original != null) {
            this.propertyValueList = new ArrayList<>(original.size());
            original.forEach((attrName, attrValue) -> this.propertyValueList.add(new PropertyValue(attrName.toString(), attrValue)));
        } else {
            this.propertyValueList = new ArrayList<>(0);
        }
    }

    /**
     * 按原样使用给定的PropertyValue对象列表构造一个新的可变PropertyValue对象。
     * 这是高级使用场景的构造函数。它不适用于典型的编程用途
     *
     * @param propertyValueList PropertyValue对象的列表
     */
    public MutablePropertyValues(List<PropertyValue> propertyValueList) {
        this.propertyValueList = (propertyValueList != null ? propertyValueList : new ArrayList<>());
    }

    /**
     * 以原始形式返回PropertyValue对象的基础列表。
     * 可以直接修改返回的列表，但不建议这样做。
     * 这是一个访问器，用于优化对所有PropertyValue对象的访问。它不适用于典型的编程用途
     */
    public List<PropertyValue> getPropertyValueList() {
        return this.propertyValueList;
    }

    /**
     * 返回列表中PropertyValue条目的数目
     */
    public int size() {
        return this.propertyValueList.size();
    }

    /**
     * 将所有给定的PropertyValue复制到此对象中。
     * 保证PropertyValue引用是独立的，尽管它不能深度复制单个PropertyValue对象当前引用的对象
     *
     * @param other 要复制的属性值
     * @return 这是为了允许在链中添加多个特性值
     */
    public MutablePropertyValues addPropertyValues(PropertyValues other) {
        if (other != null) {
            PropertyValue[] pvs = other.getPropertyValues();
            for (PropertyValue pv : pvs) {
                addPropertyValue(new PropertyValue(pv));
            }
        }
        return this;
    }

    /**
     * 添加给定Map中的所有特性值
     *
     * @param other {@code Map<属性名, 属性值>}
     */
    public MutablePropertyValues addPropertyValues(Map<?, ?> other) {
        if (other != null) {
            other.forEach((attrName, attrValue) -> addPropertyValue(new PropertyValue(attrName.toString(), attrValue)));
        }
        return this;
    }

    /**
     * 添加PropertyValue对象，替换相应属性的任何现有对象或与之合并(如果适用)
     *
     * @param pv 要添加的PropertyValue对象
     */
    public MutablePropertyValues addPropertyValue(PropertyValue pv) {
        for (int i = 0; i < this.propertyValueList.size(); i++) {
            PropertyValue currentPv = this.propertyValueList.get(i);
            if (currentPv.getName().equals(pv.getName())) {
                pv = mergeIfRequired(pv, currentPv);
                setPropertyValueAt(pv, i);
                return this;
            }
        }
        this.propertyValueList.add(pv);
        return this;
    }

    /**
     * 采用属性名称和属性值的addPropertyValue的重载版
     *
     * @param propertyName  属性名
     * @param propertyValue 属性值
     * @see #addPropertyValue(PropertyValue)
     */
    public void addPropertyValue(String propertyName, Object propertyValue) {
        addPropertyValue(new PropertyValue(propertyName, propertyValue));
    }

    /**
     * 添加PropertyValue对象，替换相应属性的任何现有对象或与之合并(如果适用)
     *
     * @param propertyName  属性名
     * @param propertyValue 属性值
     */
    public MutablePropertyValues add(String propertyName, Object propertyValue) {
        addPropertyValue(new PropertyValue(propertyName, propertyValue));
        return this;
    }

    /**
     * 修改保存在此对象中的PropertyValue对象，索引从0开始
     */
    public void setPropertyValueAt(PropertyValue pv, int i) {
        this.propertyValueList.set(i, pv);
    }

    /**
     * 如果支持并启用合并，则将提供的 'new' {@link PropertyValue}的值与当前{@link PropertyValue}的值合并
     *
     * @see Mergeable
     */
    private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
        Object value = newPv.getValue();
        if (value instanceof Mergeable) {
            Mergeable mergeable = (Mergeable) value;
            if (mergeable.isMergeEnabled()) {
                Object merged = mergeable.merge(currentPv.getValue());
                return new PropertyValue(newPv.getName(), merged);
            }
        }
        return newPv;
    }

    /**
     * 删除给定的PropertyValue(如果包含)
     *
     * @param pv 要删除的PropertyValue
     */
    public void removePropertyValue(PropertyValue pv) {
        this.propertyValueList.remove(pv);
    }

    /**
     * 采用属性名称的{@code removePropertyValue}的重载版本
     *
     * @param propertyName 属性名
     * @see #removePropertyValue(PropertyValue)
     */
    public void removePropertyValue(String propertyName) {
        this.propertyValueList.remove(getPropertyValue(propertyName));
    }

    @Override
    public Iterator<PropertyValue> iterator() {
        return Collections.unmodifiableList(this.propertyValueList).iterator();
    }

    @Override
    public Spliterator<PropertyValue> spliterator() {
        return Spliterators.spliterator(this.propertyValueList, 0);
    }

    @Override
    public Stream<PropertyValue> stream() {
        return this.propertyValueList.stream();
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return this.propertyValueList.toArray(new PropertyValue[0]);
    }

    @Override
    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue pv : this.propertyValueList) {
            if (pv.getName().equals(propertyName)) {
                return pv;
            }
        }
        return null;
    }

    /**
     * 获取原始属性值(如果有)
     *
     * @param propertyName 属性名称
     * @return 原始属性值，如果未找到，则为null
     * @see #getPropertyValue(String)
     * @see PropertyValue#getValue()
     */
    public Object get(String propertyName) {
        PropertyValue pv = getPropertyValue(propertyName);
        return (pv != null ? pv.getValue() : null);
    }

    @Override
    public PropertyValues changesSince(PropertyValues old) {
        MutablePropertyValues changes = new MutablePropertyValues();
        if (old == this) {
            return changes;
        }
        // for each property value in the new set
        for (PropertyValue newPv : this.propertyValueList) {
            // if there wasn't an old one, add it
            PropertyValue pvOld = old.getPropertyValue(newPv.getName());
            if (pvOld == null || !pvOld.equals(newPv)) {
                changes.addPropertyValue(newPv);
            }
        }
        return changes;
    }

    @Override
    public boolean contains(String propertyName) {
        return getPropertyValue(propertyName) != null || (this.processedProperties != null && this.processedProperties.contains(propertyName));
    }

    @Override
    public boolean isEmpty() {
        return this.propertyValueList.isEmpty();
    }

    /**
     * 将指定的属性注册为“已处理”，即某些处理器在PropertyValue机制之外调用相应的setter方法。
     * 这将导致从指定属性的{@link #contains}调用返回true
     *
     * @param propertyName 属性名
     */
    public void registerProcessedProperty(String propertyName) {
        if (this.processedProperties == null) {
            this.processedProperties = new HashSet<>(4);
        }
        this.processedProperties.add(propertyName);
    }

    /**
     * 清除给定属性的“已处理”标记(如果有)
     */
    public void clearProcessedProperty(String propertyName) {
        if (this.processedProperties != null) {
            this.processedProperties.remove(propertyName);
        }
    }

    /**
     * 将此持有者标记为仅包含转换的值(即不再需要运行时解析)
     */
    public void setConverted() {
        this.converted = true;
    }

    /**
     * 返回此保持器是否仅包含已转换的值(true)，或值是否仍需要转换(false)
     */
    public boolean isConverted() {
        return this.converted;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof MutablePropertyValues && this.propertyValueList.equals(((MutablePropertyValues) other).propertyValueList));
    }

    @Override
    public int hashCode() {
        return this.propertyValueList.hashCode();
    }

    @Override
    public String toString() {
        PropertyValue[] pvs = getPropertyValues();
        if (pvs.length > 0) {
            return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
        }
        return "PropertyValues: length=0";
    }
}
