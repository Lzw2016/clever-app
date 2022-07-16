package org.clever.boot.context.properties.source;

import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginProvider;
import org.clever.boot.origin.OriginTrackedValue;
import org.clever.core.style.ToStringCreator;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 从{@link ConfigurationPropertySource}获得的单个配置属性，
 * 由{@link #getName() name}、{@link #getValue() value}和可选{@link #getOrigin() origin}组成。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:35 <br/>
 */
public final class ConfigurationProperty implements OriginProvider, Comparable<ConfigurationProperty> {
    private final ConfigurationPropertyName name;
    private final Object value;
    private final ConfigurationPropertySource source;
    private final Origin origin;

    public ConfigurationProperty(ConfigurationPropertyName name, Object value, Origin origin) {
        this(null, name, value, origin);
    }

    private ConfigurationProperty(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value, Origin origin) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(value, "Value must not be null");
        this.source = source;
        this.name = name;
        this.value = value;
        this.origin = origin;
    }

    /**
     * 返回提供属性的{@link ConfigurationPropertySource}，如果源未知，则返回null。
     *
     * @return 配置属性源
     */
    public ConfigurationPropertySource getSource() {
        return this.source;
    }

    /**
     * 返回配置属性的名称。
     *
     * @return 配置属性名称
     */
    public ConfigurationPropertyName getName() {
        return this.name;
    }

    /**
     * 返回配置属性的值。
     *
     * @return 配置属性值
     */
    public Object getValue() {
        return this.value;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConfigurationProperty other = (ConfigurationProperty) obj;
        boolean result;
        result = ObjectUtils.nullSafeEquals(this.name, other.name);
        result = result && ObjectUtils.nullSafeEquals(this.value, other.value);
        return result;
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(this.name);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.value);
        return result;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this)
                .append("name", this.name)
                .append("value", this.value)
                .append("origin", this.origin)
                .toString();
    }

    @Override
    public int compareTo(ConfigurationProperty other) {
        return this.name.compareTo(other.name);
    }

    static ConfigurationProperty of(ConfigurationPropertyName name, OriginTrackedValue value) {
        if (value == null) {
            return null;
        }
        return new ConfigurationProperty(name, value.getValue(), value.getOrigin());
    }

    static ConfigurationProperty of(ConfigurationPropertySource source, ConfigurationPropertyName name, Object value, Origin origin) {
        if (value == null) {
            return null;
        }
        return new ConfigurationProperty(source, name, value, origin);
    }
}
