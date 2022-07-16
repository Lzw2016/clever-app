package org.clever.boot.origin;

import org.clever.core.env.PropertySource;
import org.clever.util.Assert;

/**
 * {@link Origin} 来自 {@link PropertySource}.
 *
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:50 <br/>
 */
public class PropertySourceOrigin implements Origin {
    private final PropertySource<?> propertySource;
    private final String propertyName;

    /**
     * 创建新的 {@link PropertySourceOrigin}
     * @param propertySource 属性来源
     * @param propertyName 属性源中的名称
     */
    public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName) {
        Assert.notNull(propertySource, "PropertySource must not be null");
        Assert.hasLength(propertyName, "PropertyName must not be empty");
        this.propertySource = propertySource;
        this.propertyName = propertyName;
    }

    /**
     * 返回源 {@link PropertySource}.
     */
    public PropertySource<?> getPropertySource() {
        return this.propertySource;
    }

    /**
     * 返回从中获取原始值时使用的属性名称 {@link #getPropertySource() 属性源}.
     * @return 原始属性名称
     */
    public String getPropertyName() {
        return this.propertyName;
    }

    @Override
    public String toString() {
        return "\"" + this.propertyName + "\" from property source \"" + this.propertySource.getName() + "\"";
    }

    /**
     * 获取给定{@link PropertySource}和{@code propertyName}的{@link Origin}。将返回{@link OriginLookup}结果或{@link PropertySourceOrigin}。
     * @param propertySource 源属性source
     * @param name 属性名称
     * @return 源属性
     */
    public static Origin get(PropertySource<?> propertySource, String name) {
        Origin origin = OriginLookup.getOrigin(propertySource, name);
        return (origin != null) ? origin : new PropertySourceOrigin(propertySource, name);
    }
}
