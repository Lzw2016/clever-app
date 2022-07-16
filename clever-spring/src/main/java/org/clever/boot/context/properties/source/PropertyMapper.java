package org.clever.boot.context.properties.source;

import org.clever.core.env.PropertySource;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * 用于提供{@link PropertySource}和{@link ConfigurationPropertySource}之间映射的策略。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:47 <br/>
 *
 * @see SpringConfigurationPropertySource
 */
interface PropertyMapper {
    /**
     * 检查的默认祖先
     */
    BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> DEFAULT_ANCESTOR_OF_CHECK = ConfigurationPropertyName::isAncestorOf;

    /**
     * 提供来自 {@link ConfigurationPropertySource} {@link ConfigurationPropertyName}.
     *
     * @param configurationPropertyName 要映射的名称
     * @return 映射的名称或空列表
     */
    List<String> map(ConfigurationPropertyName configurationPropertyName);

    /**
     * 提供来自{@link PropertySource}属性名称的映射。
     *
     * @param propertySourceName 要映射的名称
     * @return 映射的配置属性名称或 {@link ConfigurationPropertyName#EMPTY}
     */
    ConfigurationPropertyName map(String propertySourceName);

    /**
     * 返回一个{@link BiPredicate}，在考虑映射规则时，该{@link BiPredicate}可用于检查一个名称是否是另一个名称的祖先。
     *
     * @return 可以用来检查一个名称是否是另一个名称的祖先的谓词
     */
    default BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
        return DEFAULT_ANCESTOR_OF_CHECK;
    }
}
