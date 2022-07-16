package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.AbstractBindHandler;
import org.clever.boot.context.properties.bind.BindContext;
import org.clever.boot.context.properties.bind.BindHandler;
import org.clever.boot.context.properties.bind.Bindable;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.origin.Origin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link BindHandler} 设置绑定的 {@link ConfigDataLocation} 对象的 {@link Origin}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:25 <br/>
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {
    @Override
    @SuppressWarnings("unchecked")
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        if (result instanceof ConfigDataLocation) {
            return withOrigin(context, (ConfigDataLocation) result);
        }
        if (result instanceof List) {
            List<Object> list = ((List<Object>) result).stream().filter(Objects::nonNull).collect(Collectors.toList());
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element instanceof ConfigDataLocation) {
                    list.set(i, withOrigin(context, (ConfigDataLocation) element));
                }
            }
            return list;
        }
        if (result instanceof ConfigDataLocation[]) {
            ConfigDataLocation[] locations = Arrays.stream((ConfigDataLocation[]) result).filter(Objects::nonNull)
                    .toArray(ConfigDataLocation[]::new);
            for (int i = 0; i < locations.length; i++) {
                locations[i] = withOrigin(context, locations[i]);
            }
            return locations;
        }
        return result;
    }

    private ConfigDataLocation withOrigin(BindContext context, ConfigDataLocation result) {
        if (result.getOrigin() != null) {
            return result;
        }
        Origin origin = Origin.from(context.getConfigurationProperty());
        return result.withOrigin(origin);
    }
}
