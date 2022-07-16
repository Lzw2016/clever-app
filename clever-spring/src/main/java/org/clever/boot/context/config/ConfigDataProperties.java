package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.*;
import org.clever.boot.context.properties.source.ConfigurationProperty;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * 使用 {@link ConfigData} 时使用的绑定属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:21 <br/>
 */
class ConfigDataProperties {
    private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("clever.config");
    private static final ConfigurationPropertyName LEGACY_PROFILES_NAME = ConfigurationPropertyName.of("clever.profiles");
    private static final Bindable<ConfigDataProperties> BINDABLE_PROPERTIES = Bindable.of(ConfigDataProperties.class);
    private static final Bindable<String[]> BINDABLE_STRING_ARRAY = Bindable.of(String[].class);

    private final List<ConfigDataLocation> imports;
    private final Activate activate;

    /**
     * 创建新的 {@link ConfigDataProperties}
     *
     * @param imports  请求的导入
     * @param activate 激活属性
     */
    ConfigDataProperties(@Name("import") List<ConfigDataLocation> imports, Activate activate) {
        this.imports = (imports != null) ? imports : Collections.emptyList();
        this.activate = activate;
    }

    /**
     * 返回请求的任何其他进口
     *
     * @return 请求的进口
     */
    List<ConfigDataLocation> getImports() {
        return this.imports;
    }

    /**
     * 如果属性指示配置数据属性源对于给定的激活上下文是活动的，则返回 true
     *
     * @param activationContext 激活上下文
     * @return 如果配置数据属性源处于活动状态，则为 true
     */
    boolean isActive(ConfigDataActivationContext activationContext) {
        return this.activate == null || this.activate.isActive(activationContext);
    }

    /**
     * 在没有任何导入的情况下返回这些属性的新变体。
     *
     * @return 新 {@link ConfigDataProperties} 实例
     */
    ConfigDataProperties withoutImports() {
        return new ConfigDataProperties(null, this.activate);
    }

    ConfigDataProperties withLegacyProfiles(String[] legacyProfiles, ConfigurationProperty property) {
        if (this.activate != null && !ObjectUtils.isEmpty(this.activate.onProfile)) {
            throw new InvalidConfigDataPropertyException(property, false, NAME.append("activate.on-profile"), null);
        }
        return new ConfigDataProperties(this.imports, new Activate(legacyProfiles));
    }

    /**
     * 用于从给定的 {@link Binder} 创建 {@link ConfigDataProperties} 的工厂方法。
     *
     * @param binder 用于绑定属性的 Binder
     * @return {@link ConfigDataProperties} 实例或null
     */
    static ConfigDataProperties get(Binder binder) {
        LegacyProfilesBindHandler legacyProfilesBindHandler = new LegacyProfilesBindHandler();
        String[] legacyProfiles = binder.bind(LEGACY_PROFILES_NAME, BINDABLE_STRING_ARRAY, legacyProfilesBindHandler).orElse(null);
        ConfigDataProperties properties = binder.bind(NAME, BINDABLE_PROPERTIES, new ConfigDataLocationBindHandler()).orElse(null);
        if (!ObjectUtils.isEmpty(legacyProfiles)) {
            properties = (properties != null) ?
                    properties.withLegacyProfiles(legacyProfiles, legacyProfilesBindHandler.getProperty())
                    : new ConfigDataProperties(null, new Activate(legacyProfiles));
        }
        return properties;
    }

    /**
     * {@link BindHandler} 用于检查遗留处理属性
     */
    private static class LegacyProfilesBindHandler implements BindHandler {
        private ConfigurationProperty property;

        @Override
        public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
            this.property = context.getConfigurationProperty();
            return result;
        }

        ConfigurationProperty getProperty() {
            return this.property;
        }
    }

    /**
     * 激活用于确定配置数据属性源何时处于活动状态的属性
     */
    static class Activate {
        private final String[] onProfile;

        /**
         * 创建新的 {@link Activate}
         *
         * @param onProfile 激活所需的配置文件表达式
         */
        Activate(String[] onProfile) {
            this.onProfile = onProfile;
        }

        /**
         * 如果属性指示配置数据属性源在给定激活上下文中处于活动状态，则返回true。
         *
         * @param activationContext 激活上下文
         * @return 如果配置数据属性源处于活动状态，则为true
         */
        boolean isActive(ConfigDataActivationContext activationContext) {
            if (activationContext == null) {
                return false;
            }
            return isActive(activationContext.getProfiles());
        }

        private boolean isActive(Profiles profiles) {
            return ObjectUtils.isEmpty(this.onProfile) || (profiles != null && matchesActiveProfiles(profiles::isAccepted));
        }

        private boolean matchesActiveProfiles(Predicate<String> activeProfiles) {
            return org.clever.core.env.Profiles.of(this.onProfile).matches(activeProfiles);
        }
    }
}
