package org.clever.boot.context.config;

import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;
import org.clever.core.env.AbstractEnvironment;
import org.slf4j.Logger;

import java.util.*;

/**
 * 如果在处理配置数据时发现无效属性，则引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:22 <br/>
 */
public class InvalidConfigDataPropertyException extends ConfigDataException {
    private static final Map<ConfigurationPropertyName, ConfigurationPropertyName> WARNINGS;

    static {
        Map<ConfigurationPropertyName, ConfigurationPropertyName> warnings = new LinkedHashMap<>();
        warnings.put(
                ConfigurationPropertyName.of("clever.profiles"),
                ConfigurationPropertyName.of("clever.config.activate.on-profile")
        );
        warnings.put(
                ConfigurationPropertyName.of("clever.profiles[0]"),
                ConfigurationPropertyName.of("clever.config.activate.on-profile")
        );
        WARNINGS = Collections.unmodifiableMap(warnings);
    }

    private static final Set<ConfigurationPropertyName> PROFILE_SPECIFIC_ERRORS;

    static {
        Set<ConfigurationPropertyName> errors = new LinkedHashSet<>();
        errors.add(Profiles.INCLUDE_PROFILES);
        errors.add(Profiles.INCLUDE_PROFILES.append("[0]"));
        errors.add(ConfigurationPropertyName.of(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME));
        errors.add(ConfigurationPropertyName.of(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME + "[0]"));
        errors.add(ConfigurationPropertyName.of(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME));
        errors.add(ConfigurationPropertyName.of(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME + "[0]"));
        PROFILE_SPECIFIC_ERRORS = Collections.unmodifiableSet(errors);
    }

    private final ConfigurationProperty property;
    private final ConfigurationPropertyName replacement;
    private final ConfigDataResource location;

    InvalidConfigDataPropertyException(ConfigurationProperty property,
                                       boolean profileSpecific,
                                       ConfigurationPropertyName replacement,
                                       ConfigDataResource location) {
        super(getMessage(property, profileSpecific, replacement, location), null);
        this.property = property;
        this.replacement = replacement;
        this.location = location;
    }

    /**
     * 返回导致异常的源属性。
     *
     * @return 无效属性
     */
    public ConfigurationProperty getProperty() {
        return this.property;
    }

    /**
     * 返回无效属性的 {@link ConfigDataResource}，如果源不是从 {@link ConfigData} 加载的，则返回null。
     *
     * @return 配置数据位置或null
     */
    public ConfigDataResource getLocation() {
        return this.location;
    }

    /**
     * 返回应改用的替换属性，如果没有可用的替换，则返回null。
     *
     * @return 替换属性名
     */
    public ConfigurationPropertyName getReplacement() {
        return this.replacement;
    }

    /**
     * 如果给定的 {@link InvalidConfigDataPropertyException} 包含任何无效属性，则抛出 {@link ConfigDataEnvironmentContributor} 或日志警告。
     * 如果该属性仍受支持，但不建议使用，则会记录警告。如果该属性完全不受支持，则会引发错误。
     *
     * @param logger      用于警告的记录器
     * @param contributor 要检查的参与者
     */
    static void throwOrWarn(Logger logger, ConfigDataEnvironmentContributor contributor) {
        ConfigurationPropertySource propertySource = contributor.getConfigurationPropertySource();
        if (propertySource != null) {
            WARNINGS.forEach((name, replacement) -> {
                ConfigurationProperty property = propertySource.getConfigurationProperty(name);
                if (property != null) {
                    logger.warn(getMessage(property, false, replacement, contributor.getResource()));
                }
            });
            if (contributor.isFromProfileSpecificImport() && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
                PROFILE_SPECIFIC_ERRORS.forEach((name) -> {
                    ConfigurationProperty property = propertySource.getConfigurationProperty(name);
                    if (property != null) {
                        throw new InvalidConfigDataPropertyException(property, true, null, contributor.getResource());
                    }
                });
            }
        }
    }

    private static String getMessage(ConfigurationProperty property, boolean profileSpecific, ConfigurationPropertyName replacement, ConfigDataResource location) {
        StringBuilder message = new StringBuilder("Property '");
        message.append(property.getName());
        if (location != null) {
            message.append("' imported from location '");
            message.append(location);
        }
        message.append("' is invalid");
        if (profileSpecific) {
            message.append(" in a profile specific resource");
        }
        if (replacement != null) {
            message.append(" and should be replaced with '");
            message.append(replacement);
            message.append("'");
        }
        if (property.getOrigin() != null) {
            message.append(" [origin: ");
            message.append(property.getOrigin());
            message.append("]");
        }
        return message.toString();
    }
}
