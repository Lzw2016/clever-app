package org.clever.beans.propertyeditors;

import org.clever.util.Assert;
import org.clever.util.ReflectionUtils;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.*;

/**
 * {@code java.util.Collection} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:42 <br/>
 *
 * @see java.util.Collection
 * @see java.util.Set
 * @see java.util.SortedSet
 * @see java.util.List
 */
public class CustomCollectionEditor extends PropertyEditorSupport {
    @SuppressWarnings("rawtypes")
    private final Class<? extends Collection> collectionType;
    private final boolean nullAsEmptyCollection;

    /**
     * 为给定的目标类型创建一个新的CustomCollectionEditor，保持传入null不变
     *
     * @param collectionType 目标类型，需要是集合的子接口或具体集合类
     * @see java.util.Collection
     * @see java.util.ArrayList
     * @see java.util.TreeSet
     * @see java.util.LinkedHashSet
     */
    @SuppressWarnings("rawtypes")
    public CustomCollectionEditor(Class<? extends Collection> collectionType) {
        this(collectionType, false);
    }

    /**
     * 为给定的目标类型创建新的CustomCollectionEditor。
     * 默认的集合实现是：ArrayList for List, TreeSet for SortedSet, LinkedHashSet for Set
     *
     * @param collectionType        目标类型，需要是集合的子接口或具体集合类
     * @param nullAsEmptyCollection 是否将传入的null值转换为空集合（适当类型）
     * @see java.util.Collection
     * @see java.util.ArrayList
     * @see java.util.TreeSet
     * @see java.util.LinkedHashSet
     */
    @SuppressWarnings("rawtypes")
    public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
        Assert.notNull(collectionType, "Collection type is required");
        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
        }
        this.collectionType = collectionType;
        this.nullAsEmptyCollection = nullAsEmptyCollection;
    }

    /**
     * 将给定的文本值转换为具有单个元素的集合
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(text);
    }

    /**
     * 将给定值转换为目标类型的集合
     */
    @Override
    public void setValue(Object value) {
        if (value == null && this.nullAsEmptyCollection) {
            super.setValue(createCollection(this.collectionType, 0));
        } else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
            // Use the source value as-is, as it matches the target type.
            super.setValue(value);
        } else if (value instanceof Collection) {
            // Convert Collection elements.
            Collection<?> source = (Collection<?>) value;
            Collection<Object> target = createCollection(this.collectionType, source.size());
            for (Object elem : source) {
                target.add(convertElement(elem));
            }
            super.setValue(target);
        } else if (value.getClass().isArray()) {
            // Convert array elements to Collection elements.
            int length = Array.getLength(value);
            Collection<Object> target = createCollection(this.collectionType, length);
            for (int i = 0; i < length; i++) {
                target.add(convertElement(Array.get(value, i)));
            }
            super.setValue(target);
        } else {
            // A plain value: convert it to a Collection with a single element.
            Collection<Object> target = createCollection(this.collectionType, 1);
            target.add(convertElement(value));
            super.setValue(target);
        }
    }

    /**
     * 使用给定的初始容量（如果集合类型支持）创建给定类型的集合
     *
     * @param collectionType  集合的子接口
     * @param initialCapacity 初始容量
     * @return 新集合实例
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
        if (!collectionType.isInterface()) {
            try {
                return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate collection class: " + collectionType.getName(), ex);
            }
        } else if (List.class == collectionType) {
            return new ArrayList<>(initialCapacity);
        } else if (SortedSet.class == collectionType) {
            return new TreeSet<>();
        } else {
            return new LinkedHashSet<>(initialCapacity);
        }
    }

    /**
     * 返回是否始终创建新集合，即使传入集合的类型已匹配。
     * 默认为“false”；可以重写以强制创建新集合，例如在任何情况下转换元素
     *
     * @see #convertElement
     */
    protected boolean alwaysCreateNewCollection() {
        return false;
    }

    /**
     * 钩子以转换遇到的每个Collection/array元素。
     * 默认实现只是按原样返回传入的元素。
     * 可以重写以执行某些元素的转换，例如，如果输入字符串数组并应将其转换为一组整数对象，则可以将字符串转换为整数。
     * 仅在实际创建新集合时调用！如果传入集合的类型已匹配，则默认情况下不会出现这种情况。
     * 重写{@link #alwaysCreateNewCollection()}，以在任何情况下强制创建新集合
     *
     * @param element 源元素
     * @return 要在目标集合中使用的元素
     * @see #alwaysCreateNewCollection()
     */
    protected Object convertElement(Object element) {
        return element;
    }

    /**
     * 此实现返回null，表示没有适当的文本表示
     */
    @Override
    public String getAsText() {
        return null;
    }
}
