package org.clever.boot.context.properties.source;

import org.clever.util.Assert;

import java.util.function.Predicate;

/**
 * 已筛选 {@link ConfigurationPropertySource}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:43 <br/>
 */
class FilteredConfigurationPropertiesSource implements ConfigurationPropertySource {
    private final ConfigurationPropertySource source;
    private final Predicate<ConfigurationPropertyName> filter;

    FilteredConfigurationPropertiesSource(ConfigurationPropertySource source, Predicate<ConfigurationPropertyName> filter) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(filter, "Filter must not be null");
        this.source = source;
        this.filter = filter;
    }

    @Override
    public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
        boolean filtered = getFilter().test(name);
        return filtered ? getSource().getConfigurationProperty(name) : null;
    }

    @Override
    public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        ConfigurationPropertyState result = this.source.containsDescendantOf(name);
        if (result == ConfigurationPropertyState.PRESENT) {
            // We can't be sure a contained descendant won't be filtered
            return ConfigurationPropertyState.UNKNOWN;
        }
        return result;
    }

    @Override
    public Object getUnderlyingSource() {
        return this.source.getUnderlyingSource();
    }

    protected ConfigurationPropertySource getSource() {
        return this.source;
    }

    protected Predicate<ConfigurationPropertyName> getFilter() {
        return this.filter;
    }

    @Override
    public String toString() {
        return this.source.toString() + " (filtered)";
    }
}
