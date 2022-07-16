package org.clever.core.env;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 包含一个或多个{@link PropertySource}对象的持有者
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:36 <br/>
 *
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {
    /**
     * 返回包含属性源的连续{@link Stream}
     */
    default Stream<PropertySource<?>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回是否包含具有给定名称的属性源
     *
     * @param name 要查找的属性源的名称{@linkplain PropertySource#getName()}
     */
    boolean contains(String name);

    /**
     * 返回具有给定名称的属性源，如果未找到，则返回null
     * Return the property source with the given name, {@code null} if not found.
     *
     * @param name 要查找的属性源的名称{@linkplain PropertySource#getName()}
     */
    PropertySource<?> get(String name);
}
