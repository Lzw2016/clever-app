package org.clever.boot.context.properties.source;

import org.clever.util.Assert;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;

import java.util.*;

/**
 * 维护{@link ConfigurationPropertyName}别名的映射。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:45 <br/>
 *
 * @see ConfigurationPropertySource#withAliases(ConfigurationPropertyNameAliases)
 */
public final class ConfigurationPropertyNameAliases implements Iterable<ConfigurationPropertyName> {
    private final MultiValueMap<ConfigurationPropertyName, ConfigurationPropertyName> aliases = new LinkedMultiValueMap<>();

    public ConfigurationPropertyNameAliases() {
    }

    public ConfigurationPropertyNameAliases(String name, String... aliases) {
        addAliases(name, aliases);
    }

    public ConfigurationPropertyNameAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
        addAliases(name, aliases);
    }

    public void addAliases(String name, String... aliases) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(aliases, "Aliases must not be null");
        addAliases(
                ConfigurationPropertyName.of(name),
                Arrays.stream(aliases).map(ConfigurationPropertyName::of).toArray(ConfigurationPropertyName[]::new)
        );
    }

    public void addAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(aliases, "Aliases must not be null");
        this.aliases.addAll(name, Arrays.asList(aliases));
    }

    public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
        return this.aliases.getOrDefault(name, Collections.emptyList());
    }

    public ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
        return this.aliases.entrySet().stream()
                .filter((e) -> e.getValue().contains(alias)).map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    @Override
    public Iterator<ConfigurationPropertyName> iterator() {
        return this.aliases.keySet().iterator();
    }
}
