package org.clever.boot.context.properties.source;

import org.clever.util.Assert;

/**
 * 支持名称别名的 {@link ConfigurationPropertySource}
 *
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:44 <br/>
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {
    private final ConfigurationPropertySource source;
    private final ConfigurationPropertyNameAliases aliases;

    AliasedConfigurationPropertySource(ConfigurationPropertySource source, ConfigurationPropertyNameAliases aliases) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(aliases, "Aliases must not be null");
        this.source = source;
        this.aliases = aliases;
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        ConfigurationProperty result = getSource().getConfigurationProperty(name);
        if (result == null) {
            ConfigurationPropertyName aliasedName = getAliases().getNameForAlias(name);
            result = getSource().getConfigurationProperty(aliasedName);
        }
        return result;
    }

    @Override
    public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        ConfigurationPropertyState result = this.source.containsDescendantOf(name);
        if (result != ConfigurationPropertyState.ABSENT) {
            return result;
        }
        for (ConfigurationPropertyName alias : getAliases().getAliases(name)) {
            ConfigurationPropertyState aliasResult = this.source.containsDescendantOf(alias);
            if (aliasResult != ConfigurationPropertyState.ABSENT) {
                return aliasResult;
            }
        }
        for (ConfigurationPropertyName from : getAliases()) {
            for (ConfigurationPropertyName alias : getAliases().getAliases(from)) {
                if (name.isAncestorOf(alias)) {
                    if (this.source.getConfigurationProperty(from) != null) {
                        return ConfigurationPropertyState.PRESENT;
                    }
                }
            }
        }
        return ConfigurationPropertyState.ABSENT;
    }

    @Override
    public Object getUnderlyingSource() {
        return this.source.getUnderlyingSource();
    }

    protected ConfigurationPropertySource getSource() {
        return this.source;
    }

    protected ConfigurationPropertyNameAliases getAliases() {
        return this.aliases;
    }
}

