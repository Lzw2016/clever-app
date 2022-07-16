package org.clever.boot.context.properties.source;

import org.clever.core.env.AbstractPropertyResolver;
import org.clever.core.env.MutablePropertySources;
import org.clever.core.env.PropertySources;
import org.clever.core.env.PropertySourcesPropertyResolver;

/**
 * 如果名称是值{@link ConfigurationPropertyName}，则识别{@link ConfigurationPropertySourcesPropertySource}
 * 并保存对底层源的重复调用的替代{@link PropertySourcesPropertyResolver}实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:56 <br/>
 */
class ConfigurationPropertySourcesPropertyResolver extends AbstractPropertyResolver {
    private final MutablePropertySources propertySources;
    private final DefaultResolver defaultResolver;

    ConfigurationPropertySourcesPropertyResolver(MutablePropertySources propertySources) {
        this.propertySources = propertySources;
        this.defaultResolver = new DefaultResolver(propertySources);
    }

    @Override
    public boolean containsProperty(String key) {
        ConfigurationPropertySourcesPropertySource attached = getAttached();
        if (attached != null) {
            ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
            if (name != null) {
                try {
                    return attached.findConfigurationProperty(name) != null;
                } catch (Exception ignored) {
                }
            }
        }
        return this.defaultResolver.containsProperty(key);
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class, true);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetValueType) {
        return getProperty(key, targetValueType, true);
    }

    @Override
    protected String getPropertyAsRawString(String key) {
        return getProperty(key, String.class, false);
    }

    private <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        Object value = findPropertyValue(key);
        if (value == null) {
            return null;
        }
        if (resolveNestedPlaceholders && value instanceof String) {
            value = resolveNestedPlaceholders((String) value);
        }
        return convertValueIfNecessary(value, targetValueType);
    }

    private Object findPropertyValue(String key) {
        ConfigurationPropertySourcesPropertySource attached = getAttached();
        if (attached != null) {
            ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
            if (name != null) {
                try {
                    ConfigurationProperty configurationProperty = attached.findConfigurationProperty(name);
                    return (configurationProperty != null) ? configurationProperty.getValue() : null;
                } catch (Exception ignored) {
                }
            }
        }
        return this.defaultResolver.getProperty(key, Object.class, false);
    }

    private ConfigurationPropertySourcesPropertySource getAttached() {
        ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) ConfigurationPropertySources.getAttached(this.propertySources);
        Iterable<ConfigurationPropertySource> attachedSource = (attached != null) ? attached.getSource() : null;
        if ((attachedSource instanceof SpringConfigurationPropertySources) && ((SpringConfigurationPropertySources) attachedSource).isUsingSources(this.propertySources)) {
            return attached;
        }
        return null;
    }

    /**
     * 如果未连接{@link PropertySourcesPropertyResolver}，则使用默认{@link ConfigurationPropertySources}。
     */
    static class DefaultResolver extends PropertySourcesPropertyResolver {
        DefaultResolver(PropertySources propertySources) {
            super(propertySources);
        }

        @Override
        public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
            return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
        }
    }
}
