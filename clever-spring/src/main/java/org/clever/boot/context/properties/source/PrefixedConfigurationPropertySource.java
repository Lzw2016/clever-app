package org.clever.boot.context.properties.source;

import org.clever.util.Assert;

/**
 * {@link ConfigurationPropertySource} 支持前缀。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:45 <br/>
 */
class PrefixedConfigurationPropertySource implements ConfigurationPropertySource {
    private final ConfigurationPropertySource source;
    private final ConfigurationPropertyName prefix;

    PrefixedConfigurationPropertySource(ConfigurationPropertySource source, String prefix) {
        Assert.notNull(source, "Source must not be null");
        Assert.hasText(prefix, "Prefix must not be empty");
        this.source = source;
        this.prefix = ConfigurationPropertyName.of(prefix);
    }

    protected final ConfigurationPropertyName getPrefix() {
        return this.prefix;
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
        ConfigurationProperty configurationProperty = this.source.getConfigurationProperty(getPrefixedName(name));
        if (configurationProperty == null) {
            return null;
        }
        return ConfigurationProperty.of(
                configurationProperty.getSource(),
                name, configurationProperty.getValue(),
                configurationProperty.getOrigin()
        );
    }

    private ConfigurationPropertyName getPrefixedName(ConfigurationPropertyName name) {
        return this.prefix.append(name);
    }

    @Override
    public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        return this.source.containsDescendantOf(getPrefixedName(name));
    }

    @Override
    public Object getUnderlyingSource() {
        return this.source.getUnderlyingSource();
    }

    protected ConfigurationPropertySource getSource() {
        return this.source;
    }
}
