package org.clever.boot.context.properties.source;

import org.clever.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.clever.boot.origin.Origin;
import org.clever.boot.origin.PropertySourceOrigin;
import org.clever.core.env.EnumerablePropertySource;
import org.clever.core.env.PropertySource;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.env.SystemEnvironmentPropertySource;
import org.clever.util.Assert;

import java.util.Map;
import java.util.Random;

/**
 * {@link ConfigurationPropertySource}由不可枚举的{@link PropertySource}或受限的{@link EnumerablePropertySource}实现（例如安全受限的systemEnvironment源）支持。
 * {@link PropertySource}在{@link PropertyMapper}的帮助下进行调整，{@link PropertyMapper}为各个属性提供映射规则。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:47 <br/>
 *
 * @see #from(PropertySource)
 * @see PropertyMapper
 * @see SpringIterableConfigurationPropertySource
 */
class SpringConfigurationPropertySource implements ConfigurationPropertySource {
    private static final PropertyMapper[] DEFAULT_MAPPERS = {DefaultPropertyMapper.INSTANCE};
    private static final PropertyMapper[] SYSTEM_ENVIRONMENT_MAPPERS = {
            SystemEnvironmentPropertyMapper.INSTANCE, DefaultPropertyMapper.INSTANCE
    };
    private final PropertySource<?> propertySource;
    private final PropertyMapper[] mappers;

    /**
     * 创建新的{@link SpringConfigurationPropertySource}实现
     *
     * @param propertySource 源属性源
     * @param mappers        属性映射器
     */
    SpringConfigurationPropertySource(PropertySource<?> propertySource, PropertyMapper... mappers) {
        Assert.notNull(propertySource, "PropertySource must not be null");
        Assert.isTrue(mappers.length > 0, "Mappers must contain at least one item");
        this.propertySource = propertySource;
        this.mappers = mappers;
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
        if (name == null) {
            return null;
        }
        for (PropertyMapper mapper : this.mappers) {
            try {
                for (String candidate : mapper.map(name)) {
                    Object value = getPropertySource().getProperty(candidate);
                    if (value != null) {
                        Origin origin = PropertySourceOrigin.get(this.propertySource, candidate);
                        return ConfigurationProperty.of(this, name, value, origin);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        PropertySource<?> source = getPropertySource();
        if (source.getSource() instanceof Random) {
            return containsDescendantOfForRandom("random", name);
        }
        if (source.getSource() instanceof PropertySource<?> && ((PropertySource<?>) source.getSource()).getSource() instanceof Random) {
            // Assume wrapped random sources use the source name as the prefix
            return containsDescendantOfForRandom(source.getName(), name);
        }
        return ConfigurationPropertyState.UNKNOWN;
    }

    private static ConfigurationPropertyState containsDescendantOfForRandom(String prefix, ConfigurationPropertyName name) {
        if (name.getNumberOfElements() > 1 && name.getElement(0, Form.DASHED).equals(prefix)) {
            return ConfigurationPropertyState.PRESENT;
        }
        return ConfigurationPropertyState.ABSENT;
    }

    @Override
    public Object getUnderlyingSource() {
        return this.propertySource;
    }

    protected PropertySource<?> getPropertySource() {
        return this.propertySource;
    }

    protected final PropertyMapper[] getMappers() {
        return this.mappers;
    }

    @Override
    public String toString() {
        return this.propertySource.toString();
    }

    /**
     * 创建新的 {@link SpringConfigurationPropertySource} 对于指定的 {@link PropertySource}.
     *
     * @param source {@link PropertySource}
     * @return {@link SpringConfigurationPropertySource} 或 {@link SpringIterableConfigurationPropertySource} 对象
     */
    static SpringConfigurationPropertySource from(PropertySource<?> source) {
        Assert.notNull(source, "Source must not be null");
        PropertyMapper[] mappers = getPropertyMappers(source);
        if (isFullEnumerable(source)) {
            return new SpringIterableConfigurationPropertySource((EnumerablePropertySource<?>) source, mappers);
        }
        return new SpringConfigurationPropertySource(source, mappers);
    }

    private static PropertyMapper[] getPropertyMappers(PropertySource<?> source) {
        if (source instanceof SystemEnvironmentPropertySource && hasSystemEnvironmentName(source)) {
            return SYSTEM_ENVIRONMENT_MAPPERS;
        }
        return DEFAULT_MAPPERS;
    }

    private static boolean hasSystemEnvironmentName(PropertySource<?> source) {
        String name = source.getName();
        return StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name) || name.endsWith("-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    }

    private static boolean isFullEnumerable(PropertySource<?> source) {
        PropertySource<?> rootSource = getRootSource(source);
        if (rootSource.getSource() instanceof Map) {
            // Check we're not security restricted
            try {
                // noinspection ResultOfMethodCallIgnored
                ((Map<?, ?>) rootSource.getSource()).size();
            } catch (UnsupportedOperationException ex) {
                return false;
            }
        }
        return (source instanceof EnumerablePropertySource);
    }

    private static PropertySource<?> getRootSource(PropertySource<?> source) {
        while (source.getSource() != null && source.getSource() instanceof PropertySource) {
            source = (PropertySource<?>) source.getSource();
        }
        return source;
    }
}
