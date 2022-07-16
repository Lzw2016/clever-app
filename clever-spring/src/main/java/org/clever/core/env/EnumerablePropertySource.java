package org.clever.core.env;

import org.clever.util.ObjectUtils;

/**
 * 一个{@link PropertySource}实现，能够查询其底层源对象以枚举所有可能的属性name/value对。
 * 公开{@link #getPropertyNames()}方法，允许调用方内省可用属性，而无需访问底层源对象。
 * 这也有助于更高效地实现{@link #containsProperty(String)}，因为它可以调用{@link #getPropertyNames()}并遍历返回的数组，
 * 而不是尝试调用可能更昂贵的{@link #getProperty(String)}。
 * 实现可能会考虑缓存{@link #getPropertyNames()}结果，以充分利用此性能机会。
 * <p>
 * 大多数框架提供的PropertySource实现都是可枚举的；一个反例是{@code JndiPropertySource}，
 * 其中，由于JNDI的性质，不可能在任何给定时间确定所有可能的属性名称；
 * 相反，只有尝试访问属性(通过{@link #getProperty(String)})才能评估它是否存在。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:17 <br/>
 *
 * @param <T> the source type
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {
    /**
     * 新建 {@code EnumerablePropertySource} 使用给定的名称和源对象
     *
     * @param name   关联的名称
     * @param source 源对象
     */
    public EnumerablePropertySource(String name, T source) {
        super(name, source);
    }

    /**
     * 新建 {@code EnumerablePropertySource} 使用给定的名称和新的 {@code Object} 实例作为基础源
     *
     * @param name 关联的名称
     */
    protected EnumerablePropertySource(String name) {
        super(name);
    }

    /**
     * 返回此{@code PropertySource}包含具有给定名称的属性。
     * <p>此实现检查给定名称在 {@link #getPropertyNames()} 数组
     *
     * @param name 要查找的属性的名称
     */
    @Override
    public boolean containsProperty(String name) {
        return ObjectUtils.containsElement(getPropertyNames(), name);
    }

    /**
     * 返回包含的所有属性的名称{@linkplain #getSource()} 对象 (从不为null).
     */
    public abstract String[] getPropertyNames();
}
