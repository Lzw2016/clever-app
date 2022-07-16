package org.clever.boot.context.config;

import org.clever.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.clever.boot.context.properties.bind.PlaceholdersResolver;
import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginLookup;
import org.clever.core.env.PropertySource;
import org.clever.util.PropertyPlaceholderHelper;
import org.clever.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver} 由一个或多个 {@link ConfigDataEnvironmentContributor} 实例支持。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:33 <br/>
 */
class ConfigDataEnvironmentContributorPlaceholdersResolver implements PlaceholdersResolver {
    private final Iterable<ConfigDataEnvironmentContributor> contributors;
    private final ConfigDataActivationContext activationContext;
    private final boolean failOnResolveFromInactiveContributor;
    private final PropertyPlaceholderHelper helper;
    private final ConfigDataEnvironmentContributor activeContributor;

    ConfigDataEnvironmentContributorPlaceholdersResolver(Iterable<ConfigDataEnvironmentContributor> contributors,
                                                         ConfigDataActivationContext activationContext,
                                                         ConfigDataEnvironmentContributor activeContributor,
                                                         boolean failOnResolveFromInactiveContributor) {
        this.contributors = contributors;
        this.activationContext = activationContext;
        this.activeContributor = activeContributor;
        this.failOnResolveFromInactiveContributor = failOnResolveFromInactiveContributor;
        this.helper = new PropertyPlaceholderHelper(
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

    private String resolvePlaceholder(String placeholder) {
        Object result = null;
        for (ConfigDataEnvironmentContributor contributor : this.contributors) {
            PropertySource<?> propertySource = contributor.getPropertySource();
            Object value = (propertySource != null) ? propertySource.getProperty(placeholder) : null;
            if (value != null && !isActive(contributor)) {
                if (this.failOnResolveFromInactiveContributor) {
                    ConfigDataResource resource = contributor.getResource();
                    Origin origin = OriginLookup.getOrigin(propertySource, placeholder);
                    throw new InactiveConfigDataAccessException(propertySource, resource, placeholder, origin);
                }
                value = null;
            }
            result = (result != null) ? result : value;
        }
        return (result != null) ? String.valueOf(result) : null;
    }

    private boolean isActive(ConfigDataEnvironmentContributor contributor) {
        if (contributor == this.activeContributor) {
            return true;
        }
        if (contributor.getKind() != Kind.UNBOUND_IMPORT) {
            return contributor.isActive(this.activationContext);
        }
        return contributor.withBoundProperties(this.contributors, this.activationContext).isActive(this.activationContext);
    }
}