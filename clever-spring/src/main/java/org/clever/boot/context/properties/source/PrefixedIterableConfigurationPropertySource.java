package org.clever.boot.context.properties.source;

import java.util.stream.Stream;

/**
 * 一个可移植的 {@link PrefixedConfigurationPropertySource}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:53 <br/>
 */
class PrefixedIterableConfigurationPropertySource extends PrefixedConfigurationPropertySource implements IterableConfigurationPropertySource {
    PrefixedIterableConfigurationPropertySource(IterableConfigurationPropertySource source, String prefix) {
        super(source, prefix);
    }

    @Override
    public Stream<ConfigurationPropertyName> stream() {
        return getSource().stream().map(this::stripPrefix);
    }

    private ConfigurationPropertyName stripPrefix(ConfigurationPropertyName name) {
        return (getPrefix().isAncestorOf(name)) ? name.subName(getPrefix().getNumberOfElements()) : name;
    }

    @Override
    protected IterableConfigurationPropertySource getSource() {
        return (IterableConfigurationPropertySource) super.getSource();
    }
}
