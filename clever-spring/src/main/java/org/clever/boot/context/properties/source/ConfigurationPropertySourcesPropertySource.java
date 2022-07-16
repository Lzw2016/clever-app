package org.clever.boot.context.properties.source;

import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginLookup;
import org.clever.core.env.Environment;
import org.clever.core.env.PropertyResolver;
import org.clever.core.env.PropertySource;

/**
 * {@link PropertySource}，公开{@link ConfigurationPropertySource}实例，
 * 以便与{@link PropertyResolver}一起使用或添加到{@link Environment}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:46 <br/>
 */
class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>> implements OriginLookup<String> {
    ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
        super(name, source);
    }

    @Override
    public boolean containsProperty(String name) {
        return findConfigurationProperty(name) != null;
    }

    @Override
    public Object getProperty(String name) {
        ConfigurationProperty configurationProperty = findConfigurationProperty(name);
        return (configurationProperty != null) ? configurationProperty.getValue() : null;
    }

    @Override
    public Origin getOrigin(String name) {
        return Origin.from(findConfigurationProperty(name));
    }

    private ConfigurationProperty findConfigurationProperty(String name) {
        try {
            return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
        } catch (Exception ex) {
            return null;
        }
    }

    ConfigurationProperty findConfigurationProperty(ConfigurationPropertyName name) {
        if (name == null) {
            return null;
        }
        for (ConfigurationPropertySource configurationPropertySource : getSource()) {
            ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
            if (configurationProperty != null) {
                return configurationProperty;
            }
        }
        return null;
    }
}
