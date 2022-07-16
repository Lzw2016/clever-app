package org.clever.boot.context.properties.bind;

import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.Environment;
import org.clever.core.env.PropertySource;
import org.clever.core.env.PropertySources;
import org.clever.util.Assert;
import org.clever.util.PropertyPlaceholderHelper;
import org.clever.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver}从{@link PropertySources}解析占位符。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:22 <br/>
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {
    private final Iterable<PropertySource<?>> sources;
    private final PropertyPlaceholderHelper helper;

    public PropertySourcesPlaceholdersResolver(Environment environment) {
        this(getSources(environment), null);
    }

    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources) {
        this(sources, null);
    }

    public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources, PropertyPlaceholderHelper helper) {
        this.sources = sources;
        this.helper = (helper != null) ? helper : new PropertyPlaceholderHelper(
                SystemPropertyUtils.PLACEHOLDER_PREFIX,
                SystemPropertyUtils.PLACEHOLDER_SUFFIX,
                SystemPropertyUtils.VALUE_SEPARATOR,
                true
        );
    }

    @Override
    public Object resolvePlaceholders(Object value) {
        if (value instanceof String) {
            return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
        }
        return value;
    }

    protected String resolvePlaceholder(String placeholder) {
        if (this.sources != null) {
            for (PropertySource<?> source : this.sources) {
                Object value = source.getProperty(placeholder);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return null;
    }

    private static PropertySources getSources(Environment environment) {
        Assert.notNull(environment, "Environment must not be null");
        Assert.isInstanceOf(
                ConfigurableEnvironment.class, environment,
                "Environment must be a ConfigurableEnvironment"
        );
        return ((ConfigurableEnvironment) environment).getPropertySources();
    }
}
