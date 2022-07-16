package org.clever.boot.context.properties.source;

import org.clever.boot.origin.OriginTrackedValue;
import org.clever.core.env.PropertySource;
import org.clever.util.StringUtils;

import java.util.function.Predicate;

/**
 * {@link ConfigurationProperty ConfigurationProperties}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:35 <br/>
 *
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 */
@FunctionalInterface
public interface ConfigurationPropertySource {
    /**
     * 从源返回单个{@link ConfigurationProperty}，如果找不到属性，则返回null。
     *
     * @param name 属性的名称 (不能是 {@code null})
     * @return 关联对象或 {@code null}.
     */
    ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name);

    default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        return ConfigurationPropertyState.UNKNOWN;
    }

    /**
     * 返回此源的筛选变量，仅包含与给定 {@link Predicate}.
     *
     * @param filter 要匹配的筛选器
     * @return 已筛选 {@link ConfigurationPropertySource} 实例
     */
    default ConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
        return new FilteredConfigurationPropertiesSource(this, filter);
    }

    /**
     * 返回此源支持名称别名的变体。
     *
     * @param aliases 为任何给定名称返回别名流的函数
     * @return {@link ConfigurationPropertySource} 实例支持名称别名
     */
    default ConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
        return new AliasedConfigurationPropertySource(this, aliases);
    }

    /**
     * 返回此源支持前缀的变体。
     *
     * @param prefix 源中属性的前缀
     * @return {@link ConfigurationPropertySource} 支持前缀的实例
     */
    default ConfigurationPropertySource withPrefix(String prefix) {
        return (StringUtils.hasText(prefix)) ? new PrefixedConfigurationPropertySource(this, prefix) : this;
    }

    /**
     * 返回实际提供属性的基础源。
     *
     * @return 标的房地产来源或 {@code null}.
     */
    default Object getUnderlyingSource() {
        return null;
    }

    /**
     * 返回一个从给定 {@link PropertySource} 改编的新 {@link ConfigurationPropertySource}，如果无法改编源，则返回null。
     *
     * @param source 要适应的属性源
     * @return 自适应源或 {@code null} {@link SpringConfigurationPropertySource}
     */
    static ConfigurationPropertySource from(PropertySource<?> source) {
        if (source instanceof ConfigurationPropertySourcesPropertySource) {
            return null;
        }
        return SpringConfigurationPropertySource.from(source);
    }
}
