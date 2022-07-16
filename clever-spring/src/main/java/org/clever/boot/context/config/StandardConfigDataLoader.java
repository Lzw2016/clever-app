package org.clever.boot.context.config;

import org.clever.boot.context.config.ConfigData.Option;
import org.clever.boot.context.config.ConfigData.PropertySourceOptions;
import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginTrackedResource;
import org.clever.core.env.PropertySource;
import org.clever.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * {@link ConfigDataLoader} 用于 {@link Resource} 支持的位置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/08 23:14 <br/>
 */
public class StandardConfigDataLoader implements ConfigDataLoader<StandardConfigDataResource> {
    private static final PropertySourceOptions PROFILE_SPECIFIC = PropertySourceOptions.always(Option.PROFILE_SPECIFIC);
    private static final PropertySourceOptions NON_PROFILE_SPECIFIC = PropertySourceOptions.ALWAYS_NONE;

    @Override
    public ConfigData load(ConfigDataLoaderContext context, StandardConfigDataResource resource) throws IOException, ConfigDataNotFoundException {
        if (resource.isEmptyDirectory()) {
            return ConfigData.EMPTY;
        }
        ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, resource.getResource());
        StandardConfigDataReference reference = resource.getReference();
        Resource originTrackedResource = OriginTrackedResource.of(
                resource.getResource(), Origin.from(reference.getConfigDataLocation())
        );
        String name = String.format("Config resource '%s' via location '%s'", resource, reference.getConfigDataLocation());
        List<PropertySource<?>> propertySources = reference.getPropertySourceLoader().load(name, originTrackedResource);
        PropertySourceOptions options = (resource.getProfile() != null) ? PROFILE_SPECIFIC : NON_PROFILE_SPECIFIC;
        return new ConfigData(propertySources, options);
    }
}
