package org.clever.boot.context.properties.source;

import org.clever.boot.origin.OriginTrackedValue;
import org.clever.util.StringUtils;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 一个{@link ConfigurationPropertySource}，包含完整的{@link Iterable}个条目集。
 * 该接口的实现必须能够迭代所有包含的配置属性。
 * 来自{@link #getConfigurationProperty(ConfigurationPropertyName)}的任何非空结果也必须在{@link #iterator() iterator}具有等效项。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:52 <br/>
 *
 * @see ConfigurationPropertyName
 * @see OriginTrackedValue
 * @see #getConfigurationProperty(ConfigurationPropertyName)
 * @see #iterator()
 * @see #stream()
 */
public interface IterableConfigurationPropertySource extends ConfigurationPropertySource, Iterable<ConfigurationPropertyName> {
    /**
     * 返回此源管理的{@link ConfigurationPropertyName 名称}的迭代器。
     *
     * @return 迭代器 (从不为 {@code null})
     */
    @Override
    default Iterator<ConfigurationPropertyName> iterator() {
        return stream().iterator();
    }

    /**
     * 返回此源管理的{@link ConfigurationPropertyName 名称}的序列流。
     *
     * @return 一连串的名字 (从不为 {@code null})
     */
    Stream<ConfigurationPropertyName> stream();

    @Override
    default ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
        return ConfigurationPropertyState.search(this, name::isAncestorOf);
    }

    @Override
    default IterableConfigurationPropertySource filter(Predicate<ConfigurationPropertyName> filter) {
        return new FilteredIterableConfigurationPropertiesSource(this, filter);
    }

    @Override
    default IterableConfigurationPropertySource withAliases(ConfigurationPropertyNameAliases aliases) {
        return new AliasedIterableConfigurationPropertySource(this, aliases);
    }

    @Override
    default IterableConfigurationPropertySource withPrefix(String prefix) {
        return (StringUtils.hasText(prefix)) ? new PrefixedIterableConfigurationPropertySource(this, prefix) : this;
    }
}

