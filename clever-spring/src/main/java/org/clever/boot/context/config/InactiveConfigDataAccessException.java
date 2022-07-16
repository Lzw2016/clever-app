package org.clever.boot.context.config;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;
import org.clever.boot.origin.Origin;
import org.clever.core.env.PropertySource;

/**
 * 尝试针对非活动 {@link ConfigData} 属性源解析属性时引发异常。
 * 用于确保用户不会意外尝试指定无法解析的属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:34 <br/>
 */
public class InactiveConfigDataAccessException extends ConfigDataException {
    private final PropertySource<?> propertySource;
    private final ConfigDataResource location;
    private final String propertyName;
    private final Origin origin;

    /**
     * 创建新的 {@link InactiveConfigDataAccessException}
     *
     * @param propertySource 非活动属性源
     * @param location       属性源的{@link ConfigDataResource}，如果源不是从{@link ConfigData}加载的，则为null
     * @param propertyName   属性的名称
     * @param origin         原点或属性或null
     */
    InactiveConfigDataAccessException(PropertySource<?> propertySource, ConfigDataResource location, String propertyName, Origin origin) {
        super(getMessage(propertySource, location, propertyName, origin), null);
        this.propertySource = propertySource;
        this.location = location;
        this.propertyName = propertyName;
        this.origin = origin;
    }

    private static String getMessage(PropertySource<?> propertySource, ConfigDataResource location, String propertyName, Origin origin) {
        StringBuilder message = new StringBuilder("Inactive property source '");
        message.append(propertySource.getName());
        if (location != null) {
            message.append("' imported from location '");
            message.append(location);
        }
        message.append("' cannot contain property '");
        message.append(propertyName);
        message.append("'");
        if (origin != null) {
            message.append(" [origin: ");
            message.append(origin);
            message.append("]");
        }
        return message.toString();
    }

    /**
     * 返回包含属性的非活动属性源。
     *
     * @return property source
     */
    public PropertySource<?> getPropertySource() {
        return this.propertySource;
    }

    /**
     * 返回属性源的ConfigDataResource，如果源不是从ConfigData加载的，则返回null。
     *
     * @return 配置数据位置或null
     */
    public ConfigDataResource getLocation() {
        return this.location;
    }

    /**
     * 返回属性的名称。
     *
     * @return 属性名称
     */
    public String getPropertyName() {
        return this.propertyName;
    }

    /**
     * 返回原点或属性或null
     *
     * @return property origin
     */
    public Origin getOrigin() {
        return this.origin;
    }

    /**
     * 如果给定的ConfigDataEnvironmentContributor包含属性，则引发InactiveConfigDataAccessException。
     *
     * @param contributor 要检查的参与者
     * @param name        要检查的名称
     */
    static void throwIfPropertyFound(ConfigDataEnvironmentContributor contributor, ConfigurationPropertyName name) {
        ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
        ConfigurationProperty property = (source != null) ? source.getConfigurationProperty(name) : null;
        if (property != null) {
            PropertySource<?> propertySource = contributor.getPropertySource();
            ConfigDataResource location = contributor.getResource();
            throw new InactiveConfigDataAccessException(propertySource, location, name.toString(), property.getOrigin());
        }
    }
}
