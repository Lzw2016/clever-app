package org.clever.beans;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 包含一个或多个{@link PropertyValue}对象的Holder，通常包含特定目标bean的一个更新
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 16:06 <br/>
 *
 * @see PropertyValue
 */
public interface PropertyValues extends Iterable<PropertyValue> {

    /**
     * 返回属性值的{@link Iterator}
     */
    @Override
    default Iterator<PropertyValue> iterator() {
        return Arrays.asList(getPropertyValues()).iterator();
    }

    /**
     * 返回属性值上的{@link Spliterator}
     */
    @Override
    default Spliterator<PropertyValue> spliterator() {
        return Spliterators.spliterator(getPropertyValues(), 0);
    }

    /**
     * 返回包含属性值的{@link Stream}
     */
    default Stream<PropertyValue> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回保存在此对象中的PropertyValue对象的数组
     */
    PropertyValue[] getPropertyValues();

    /**
     * 返回具有给定名称的属性值(如果有)
     *
     * @param propertyName 要搜索的名称
     * @return 属性值，如果没有，则为null
     */
    PropertyValue getPropertyValue(String propertyName);

    /**
     * 返回自上一个PropertyValue以来的更改。子类还应重写equals
     *
     * @param old 旧属性值
     * @return 更新的或新的属性。如果没有更改，则返回空的PropertyValue
     * @see Object#equals
     */
    PropertyValues changesSince(PropertyValues old);

    /**
     * 此属性是否有属性值(或其他处理条目)？
     *
     * @param propertyName 属性名
     * @return 此属性是否有属性值
     */
    boolean contains(String propertyName);

    /**
     * 这个持有者根本不包含任何PropertyValue对象吗？
     */
    boolean isEmpty();
}
