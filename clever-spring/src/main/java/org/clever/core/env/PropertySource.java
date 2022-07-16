package org.clever.core.env;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表示name/value属性对源的抽象基类。
 * 底层源对象可以是封装属性的任何类型T
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:34 <br/>
 *
 * @param <T> the source type
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 */
public abstract class PropertySource<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String name;
    protected final T source;

    /**
     * 新建{@code PropertySource}使用给定的名称和源对象
     *
     * @param name   关联的名称
     * @param source 源对象
     */
    public PropertySource(String name, T source) {
        Assert.hasText(name, "Property source name must contain at least one character");
        Assert.notNull(source, "Property source must not be null");
        this.name = name;
        this.source = source;
    }

    /**
     * 新建 {@code PropertySource} 使用给定的名称和新的 {@code Object} 实例作为基础源。
     * <p>当创建从不查询实际源而是返回硬编码值的匿名实现时，在测试场景中通常很有用
     */
    @SuppressWarnings("unchecked")
    public PropertySource(String name) {
        this(name, (T) new Object());
    }

    /**
     * 返回此{@code PropertySource}的名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回此{@code PropertySource}的基础源对象
     */
    public T getSource() {
        return this.source;
    }

    /**
     * 返回此{@code PropertySource}是否包含给定名称。
     * 此实现只检查{@link #getProperty(String)}的null返回值。
     * 如果可能的话，子类可能希望实现更高效的算法
     *
     * @param name 要查找的属性名称
     */
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * 返回与给定名称关联的值，如果找不到，则返回null
     *
     * @param name 要查找的属性
     * @see PropertyResolver#getRequiredProperty(String)
     */
    public abstract Object getProperty(String name);

    /**
     * 如果满足以下条件，则此PropertySource对象等于给定对象：
     * <ul>
     * <li>它们是相同的实例
     * <li>两个对象的名称属性相等
     * </ul>
     * <p>不会计算名称以外的任何属性
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof PropertySource && ObjectUtils.nullSafeEquals(getName(), ((PropertySource<?>) other).getName())));
    }

    /**
     * 返回从此PropertySource对象的name属性派生的hashCode
     */
    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(getName());
    }

    /**
     * 如果当前日志级别不包括调试，则生成简明的输出（类型和名称）。
     * 如果启用了调试，则生成详细的输出，包括PropertySource实例的哈希代码和每个name/value属性对。
     * 此变量详细性非常有用，因为系统属性或环境变量等属性源可能包含任意数量的属性对，这可能导致难以读取异常和日志消息。
     *
     * @see Logger#isDebugEnabled()
     */
    @Override
    public String toString() {
        if (logger.isDebugEnabled()) {
            return getClass().getSimpleName() +
                    "@" + System.identityHashCode(this) +
                    " {name='" + getName() + "', properties=" + getSource() + "}";
        } else {
            return getClass().getSimpleName() + " {name='" + getName() + "'}";
        }
    }

    /**
     * 返回仅用于集合比较目的的{@code PropertySource}实现。
     * 主要用于内部使用，但给定{@code PropertySource}对象的集合，可以按如下方式使用：
     * <pre>{@code
     * List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
     * sources.add(new MapPropertySource("sourceA", mapA));
     * sources.add(new MapPropertySource("sourceB", mapB));
     * assert sources.contains(PropertySource.named("sourceA"));
     * assert sources.contains(PropertySource.named("sourceB"));
     * assert !sources.contains(PropertySource.named("sourceC"));
     * }</pre>
     * 如果调用了{@code equals(Object)}, {@code hashCode()}, {@code toString()}以外的任何方法，
     * 则返回的PropertySource将引发{@code UnsupportedOperationException}
     *
     * @param name 要创建和返回的比较PropertySource的名称
     */
    public static PropertySource<?> named(String name) {
        return new ComparisonPropertySource(name);
    }

    /**
     * 在应用程序上下文创建时无法立即初始化实际属性源的情况下，将PropertySource用作占位符。
     * 例如，基于ServletContext的属性源必须等待ServletContext对象对其封闭的ApplicationContext可用。
     * 在这种情况下，应使用存根来保存属性源的预期默认位置/顺序，然后在上下文刷新期间替换
     */
    public static class StubPropertySource extends PropertySource<Object> {
        public StubPropertySource(String name) {
            super(name, new Object());
        }

        /**
         * 始终返回 {@code null}.
         */
        @Override
        public String getProperty(String name) {
            return null;
        }
    }

    /**
     * 用于集合比较目的的{@code PropertySource}实现。
     *
     * @see PropertySource#named(String)
     */
    static class ComparisonPropertySource extends StubPropertySource {
        private static final String USAGE_ERROR = "ComparisonPropertySource instances are for use with collection comparison only";

        public ComparisonPropertySource(String name) {
            super(name);
        }

        @Override
        public Object getSource() {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public boolean containsProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public String getProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }
    }
}
