package org.clever.boot.context.properties.source;

import org.clever.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * 默认{@link PropertyMapper}实现。
 * 通过删除无效字符并转换为小写来映射名称。
 * 例如，“my.server_name.PORT”映射到“my.servername.port”。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:48 <br/>
 *
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class DefaultPropertyMapper implements PropertyMapper {
    public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

    private LastMapping<ConfigurationPropertyName, List<String>> lastMappedConfigurationPropertyName;
    private LastMapping<String, ConfigurationPropertyName> lastMappedPropertyName;

    private DefaultPropertyMapper() {
    }

    @Override
    public List<String> map(ConfigurationPropertyName configurationPropertyName) {
        // Use a local copy in case another thread changes things
        LastMapping<ConfigurationPropertyName, List<String>> last = this.lastMappedConfigurationPropertyName;
        if (last != null && last.isFrom(configurationPropertyName)) {
            return last.getMapping();
        }
        String convertedName = configurationPropertyName.toString();
        List<String> mapping = Collections.singletonList(convertedName);
        this.lastMappedConfigurationPropertyName = new LastMapping<>(configurationPropertyName, mapping);
        return mapping;
    }

    @Override
    public ConfigurationPropertyName map(String propertySourceName) {
        // Use a local copy in case another thread changes things
        LastMapping<String, ConfigurationPropertyName> last = this.lastMappedPropertyName;
        if (last != null && last.isFrom(propertySourceName)) {
            return last.getMapping();
        }
        ConfigurationPropertyName mapping = tryMap(propertySourceName);
        this.lastMappedPropertyName = new LastMapping<>(propertySourceName, mapping);
        return mapping;
    }

    private ConfigurationPropertyName tryMap(String propertySourceName) {
        try {
            ConfigurationPropertyName convertedName = ConfigurationPropertyName.adapt(propertySourceName, '.');
            if (!convertedName.isEmpty()) {
                return convertedName;
            }
        } catch (Exception ignored) {
        }
        return ConfigurationPropertyName.EMPTY;
    }

    private static class LastMapping<T, M> {
        private final T from;
        private final M mapping;

        LastMapping(T from, M mapping) {
            this.from = from;
            this.mapping = mapping;
        }

        boolean isFrom(T from) {
            return ObjectUtils.nullSafeEquals(from, this.from);
        }

        M getMapping() {
            return this.mapping;
        }
    }
}
