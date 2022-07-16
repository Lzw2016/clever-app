package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.context.properties.bind.PlaceholdersResolver;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;
import org.clever.core.env.Environment;
import org.clever.core.env.PropertySource;
import org.clever.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 可直接或间接向 {@link Environment} 提供配置数据的单个元件。
 * contributor有几个不同的 {@link Kind kinds}，它们都是不可变的，在处理导入时将被新版本替换。
 * <p>
 * contributor 可以提供一组导入，这些导入应该被处理并最终转化为子项。有两个不同的导入阶段：
 * <ul>
 * <li>在激活配置文件 {@link ImportPhase#BEFORE_PROFILE_ACTIVATION Before}</li>
 * <li>在激活配置文件 {@link ImportPhase#AFTER_PROFILE_ACTIVATION After}</li>
 * </ul>
 * 在每个阶段，所有导入都将在加载之前解析。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:20 <br/>
 */
class ConfigDataEnvironmentContributor implements Iterable<ConfigDataEnvironmentContributor> {
    private static final ConfigData.Options EMPTY_LOCATION_OPTIONS = ConfigData.Options.of(ConfigData.Option.IGNORE_IMPORTS);

    private final ConfigDataLocation location;
    private final ConfigDataResource resource;
    private final boolean fromProfileSpecificImport;
    private final PropertySource<?> propertySource;
    private final ConfigurationPropertySource configurationPropertySource;
    private final ConfigDataProperties properties;
    private final ConfigData.Options configDataOptions;
    private final Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children;
    private final Kind kind;

    /**
     * 创建新的 {@link ConfigDataEnvironmentContributor}
     *
     * @param kind                        contributor 类型
     * @param location                    此 contributor 的位置
     * @param resource                    提供数据的资源或null
     * @param fromProfileSpecificImport   如果参与者来自特定于配置文件的导入
     * @param propertySource              数据的属性源或null
     * @param configurationPropertySource 数据的配置属性源或null
     * @param properties                  配置数据属性或null
     * @param configDataOptions           应用的任何配置数据选项
     * @param children                    每个 {@link ImportPhase} 的该 contributor 的孩子
     */
    ConfigDataEnvironmentContributor(Kind kind,
                                     ConfigDataLocation location,
                                     ConfigDataResource resource,
                                     boolean fromProfileSpecificImport, PropertySource<?> propertySource,
                                     ConfigurationPropertySource configurationPropertySource,
                                     ConfigDataProperties properties,
                                     ConfigData.Options configDataOptions,
                                     Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children) {
        this.kind = kind;
        this.location = location;
        this.resource = resource;
        this.fromProfileSpecificImport = fromProfileSpecificImport;
        this.properties = properties;
        this.propertySource = propertySource;
        this.configurationPropertySource = configurationPropertySource;
        this.configDataOptions = (configDataOptions != null) ? configDataOptions : ConfigData.Options.NONE;
        this.children = (children != null) ? children : Collections.emptyMap();
    }

    /**
     * 返回参与者种类。
     *
     * @return contributor 的类型
     */
    Kind getKind() {
        return this.kind;
    }

    ConfigDataLocation getLocation() {
        return this.location;
    }

    /**
     * 如果此参与者当前处于活动状态，则返回。
     *
     * @param activationContext 激活上下文
     * @return 如果参与者处于活动状态
     */
    boolean isActive(ConfigDataActivationContext activationContext) {
        if (this.kind == Kind.UNBOUND_IMPORT) {
            return false;
        }
        return this.properties == null || this.properties.isActive(activationContext);
    }

    /**
     * 返回贡献此实例的资源。
     *
     * @return 资源或null
     */
    ConfigDataResource getResource() {
        return this.resource;
    }

    /**
     * 如果参与者来自特定于配置文件的导入，则返回。
     *
     * @return 如果参与者是特定于配置文件的
     */
    boolean isFromProfileSpecificImport() {
        return this.fromProfileSpecificImport;
    }

    /**
     * 返回此参与者的属性源。
     *
     * @return 属性源或null
     */
    PropertySource<?> getPropertySource() {
        return this.propertySource;
    }

    /**
     * 返回此参与者的配置属性源。
     *
     * @return 配置属性源或null
     */
    ConfigurationPropertySource getConfigurationPropertySource() {
        return this.configurationPropertySource;
    }

    /**
     * 如果参与者具有特定的配置数据选项，则返回。
     *
     * @param option 检查选项
     * @return 如果选项存在，则为true
     */
    boolean hasConfigDataOption(ConfigData.Option option) {
        return this.configDataOptions.contains(option);
    }

    @SuppressWarnings("SameParameterValue")
    ConfigDataEnvironmentContributor withoutConfigDataOption(ConfigData.Option option) {
        return new ConfigDataEnvironmentContributor(
                this.kind,
                this.location,
                this.resource,
                this.fromProfileSpecificImport,
                this.propertySource,
                this.configurationPropertySource,
                this.properties,
                this.configDataOptions.without(option),
                this.children
        );
    }

    /**
     * 返回此 contributor 请求的任何导入。
     */
    List<ConfigDataLocation> getImports() {
        return (this.properties != null) ? this.properties.getImports() : Collections.emptyList();
    }

    /**
     * 如果此参与者有尚未在给定阶段处理的导入，则返回true
     *
     * @param importPhase 导入阶段
     */
    boolean hasUnprocessedImports(ImportPhase importPhase) {
        if (getImports().isEmpty()) {
            return false;
        }
        return !this.children.containsKey(importPhase);
    }

    /**
     * 返回给定阶段此参与者的子级。
     *
     * @param importPhase 导入阶段
     * @return children
     */
    List<ConfigDataEnvironmentContributor> getChildren(ImportPhase importPhase) {
        return this.children.getOrDefault(importPhase, Collections.emptyList());
    }

    /**
     * 返回按优先级顺序遍历此参与者及其所有子级的 {@link Stream}
     *
     * @return stream
     */
    Stream<ConfigDataEnvironmentContributor> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回一个 {@link Iterator}，该迭代器按优先级顺序遍历此 contributor 及其所有子级。
     *
     * @return 迭代器
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<ConfigDataEnvironmentContributor> iterator() {
        return new ContributorIterator();
    }

    /**
     * 使用绑定的 {@link ConfigDataProperties} 创建新的 {@link ConfigDataEnvironmentContributor}
     *
     * @param contributors      用于绑定的 contributor
     * @param activationContext 激活上下文
     * @return 一个新的 contributor 实例
     */
    ConfigDataEnvironmentContributor withBoundProperties(Iterable<ConfigDataEnvironmentContributor> contributors, ConfigDataActivationContext activationContext) {
        Iterable<ConfigurationPropertySource> sources = Collections.singleton(getConfigurationPropertySource());
        PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
                contributors, activationContext, this, true
        );
        Binder binder = new Binder(sources, placeholdersResolver, null, null, null);
        UseLegacyConfigProcessingException.throwIfRequested(binder);
        ConfigDataProperties properties = ConfigDataProperties.get(binder);
        if (properties != null && this.configDataOptions.contains(ConfigData.Option.IGNORE_IMPORTS)) {
            properties = properties.withoutImports();
        }
        return new ConfigDataEnvironmentContributor(
                Kind.BOUND_IMPORT,
                this.location,
                this.resource,
                this.fromProfileSpecificImport,
                this.propertySource,
                this.configurationPropertySource,
                properties,
                this.configDataOptions, null
        );
    }

    /**
     * 为给定阶段创建一个新的 {@link ConfigDataEnvironmentContributor} 实例，该实例具有一组新的子实例。
     *
     * @param importPhase 导入阶段
     * @param children    新 children
     * @return 一个新的 contributor 实例
     */
    ConfigDataEnvironmentContributor withChildren(ImportPhase importPhase, List<ConfigDataEnvironmentContributor> children) {
        Map<ImportPhase, List<ConfigDataEnvironmentContributor>> updatedChildren = new LinkedHashMap<>(this.children);
        updatedChildren.put(importPhase, children);
        if (importPhase == ImportPhase.AFTER_PROFILE_ACTIVATION) {
            moveProfileSpecific(updatedChildren);
        }
        return new ConfigDataEnvironmentContributor(
                this.kind,
                this.location,
                this.resource,
                this.fromProfileSpecificImport,
                this.propertySource,
                this.configurationPropertySource,
                this.properties,
                this.configDataOptions,
                updatedChildren
        );
    }

    private void moveProfileSpecific(Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children) {
        List<ConfigDataEnvironmentContributor> before = children.get(ImportPhase.BEFORE_PROFILE_ACTIVATION);
        if (!hasAnyProfileSpecificChildren(before)) {
            return;
        }
        List<ConfigDataEnvironmentContributor> updatedBefore = new ArrayList<>(before.size());
        List<ConfigDataEnvironmentContributor> updatedAfter = new ArrayList<>();
        for (ConfigDataEnvironmentContributor contributor : before) {
            updatedBefore.add(moveProfileSpecificChildren(contributor, updatedAfter));
        }
        updatedAfter.addAll(children.getOrDefault(ImportPhase.AFTER_PROFILE_ACTIVATION, Collections.emptyList()));
        children.put(ImportPhase.BEFORE_PROFILE_ACTIVATION, updatedBefore);
        children.put(ImportPhase.AFTER_PROFILE_ACTIVATION, updatedAfter);
    }

    private ConfigDataEnvironmentContributor moveProfileSpecificChildren(ConfigDataEnvironmentContributor contributor, List<ConfigDataEnvironmentContributor> removed) {
        for (ImportPhase importPhase : ImportPhase.values()) {
            List<ConfigDataEnvironmentContributor> children = contributor.getChildren(importPhase);
            List<ConfigDataEnvironmentContributor> updatedChildren = new ArrayList<>(children.size());
            for (ConfigDataEnvironmentContributor child : children) {
                if (child.hasConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC)) {
                    removed.add(child.withoutConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC));
                } else {
                    updatedChildren.add(child);
                }
            }
            contributor = contributor.withChildren(importPhase, updatedChildren);
        }
        return contributor;
    }

    private boolean hasAnyProfileSpecificChildren(List<ConfigDataEnvironmentContributor> contributors) {
        if (CollectionUtils.isEmpty(contributors)) {
            return false;
        }
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            for (ImportPhase importPhase : ImportPhase.values()) {
                if (contributor.getChildren(importPhase).stream().anyMatch((child) -> child.hasConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 创建一个新的 {@link ConfigDataEnvironmentContributor} 实例，在其中替换现有子项。
     *
     * @param existing    应该替换的现有节点
     * @param replacement 应该使用的替换节点
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    ConfigDataEnvironmentContributor withReplacement(ConfigDataEnvironmentContributor existing, ConfigDataEnvironmentContributor replacement) {
        if (this == existing) {
            return replacement;
        }
        Map<ImportPhase, List<ConfigDataEnvironmentContributor>> updatedChildren = new LinkedHashMap<>(this.children.size());
        this.children.forEach((importPhase, contributors) -> {
            List<ConfigDataEnvironmentContributor> updatedContributors = new ArrayList<>(contributors.size());
            for (ConfigDataEnvironmentContributor contributor : contributors) {
                updatedContributors.add(contributor.withReplacement(existing, replacement));
            }
            updatedChildren.put(importPhase, Collections.unmodifiableList(updatedContributors));
        });
        return new ConfigDataEnvironmentContributor(
                this.kind,
                this.location,
                this.resource,
                this.fromProfileSpecificImport,
                this.propertySource,
                this.configurationPropertySource,
                this.properties,
                this.configDataOptions,
                updatedChildren
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildToString("", builder);
        return builder.toString();
    }

    private void buildToString(String prefix, StringBuilder builder) {
        builder.append(prefix);
        builder.append(this.kind);
        builder.append(" ");
        builder.append(this.location);
        builder.append(" ");
        builder.append(this.resource);
        builder.append(" ");
        builder.append(this.configDataOptions);
        builder.append("\n");
        for (ConfigDataEnvironmentContributor child : this.children.getOrDefault(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.emptyList())) {
            child.buildToString(prefix + "    ", builder);
        }
        for (ConfigDataEnvironmentContributor child : this.children.getOrDefault(ImportPhase.AFTER_PROFILE_ACTIVATION, Collections.emptyList())) {
            child.buildToString(prefix + "    ", builder);
        }
    }

    /**
     * 创建 {@link Kind#ROOT root} contributor 的工厂方法。
     *
     * @param contributors 根的直接孩子
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    static ConfigDataEnvironmentContributor of(List<ConfigDataEnvironmentContributor> contributors) {
        Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children = new LinkedHashMap<>();
        children.put(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.unmodifiableList(contributors));
        return new ConfigDataEnvironmentContributor(
                Kind.ROOT,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                children
        );
    }

    /**
     * 创建 {@link Kind#INITIAL_IMPORT 初始导入} contributor 的工厂方法。
     * 此 contributor 用于触发其他 contributor 的初始导入。它本身不贡献任何属性
     *
     * @param initialImport 初始导入位置（已解析占位符）
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    static ConfigDataEnvironmentContributor ofInitialImport(ConfigDataLocation initialImport) {
        List<ConfigDataLocation> imports = Collections.singletonList(initialImport);
        ConfigDataProperties properties = new ConfigDataProperties(imports, null);
        return new ConfigDataEnvironmentContributor(
                Kind.INITIAL_IMPORT,
                null,
                null,
                false,
                null,
                null,
                properties,
                null,
                null
        );
    }

    /**
     * 创建包装 {@link Kind#EXISTING 现有} 属性源的 contributor 的工厂方法。
     * contributor 提供对现有属性的访问，但不会主动导入任何其他 contributor
     *
     * @param propertySource 要包装的属性源
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    static ConfigDataEnvironmentContributor ofExisting(PropertySource<?> propertySource) {
        return new ConfigDataEnvironmentContributor(
                Kind.EXISTING,
                null,
                null,
                false,
                propertySource,
                ConfigurationPropertySource.from(propertySource),
                null,
                null,
                null
        );
    }

    /**
     * 创建 {@link Kind#UNBOUND_IMPORT 未绑定导入} contributor 的工厂方法。
     * 此 contributor 已从另一个 contributor 主动导入，并且稍后可能会导入更多 contributor
     *
     * @param location            该 contributor 的位置
     * @param resource            配置数据资源
     * @param profileSpecific     如果 contributor 来自配置文件特定的导入
     * @param configData          配置数据
     * @param propertySourceIndex 应该使用的属性源的索引
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    static ConfigDataEnvironmentContributor ofUnboundImport(ConfigDataLocation location,
                                                            ConfigDataResource resource,
                                                            boolean profileSpecific,
                                                            ConfigData configData,
                                                            int propertySourceIndex) {
        PropertySource<?> propertySource = configData.getPropertySources().get(propertySourceIndex);
        ConfigData.Options options = configData.getOptions(propertySource);
        ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(propertySource);
        return new ConfigDataEnvironmentContributor(
                Kind.UNBOUND_IMPORT,
                location,
                resource,
                profileSpecific,
                propertySource,
                configurationPropertySource,
                null,
                options,
                null
        );
    }

    /**
     * 创建 {@link Kind#EMPTY_LOCATION 空位置} contributor 的工厂方法
     *
     * @param location        该 contributor 的位置
     * @param profileSpecific 如果 contributor 来自配置文件特定的导入
     * @return 一个新的 {@link ConfigDataEnvironmentContributor} 实例
     */
    static ConfigDataEnvironmentContributor ofEmptyLocation(ConfigDataLocation location, boolean profileSpecific) {
        return new ConfigDataEnvironmentContributor(
                Kind.EMPTY_LOCATION,
                location,
                null,
                profileSpecific,
                null,
                null,
                null,
                EMPTY_LOCATION_OPTIONS,
                null
        );
    }

    /**
     * 各种 contributor
     */
    enum Kind {
        /**
         * 使用的根 contributor 包含初始子集。
         */
        ROOT,
        /**
         * 需要处理的初始导入。
         */
        INITIAL_IMPORT,
        /**
         * 提供属性但不导入的现有属性源。
         */
        EXISTING,
        /**
         * 具有从另一个 contributor 导入但尚未绑定的 {@link ConfigData} 的 contributor 。
         */
        UNBOUND_IMPORT,
        /**
         * 具有从另一个 contributor 导入的 {@link ConfigData} 的 contributor
         */
        BOUND_IMPORT,
        /**
         * 一个有效的位置，不包含任何要加载的内容。
         */
        EMPTY_LOCATION;
    }

    /**
     * 获取导入时可以使用的导入阶段。
     */
    enum ImportPhase {
        /**
         * 激活配置文件之前的阶段。
         */
        BEFORE_PROFILE_ACTIVATION,
        /**
         * 配置文件被激活后的阶段。
         */
        AFTER_PROFILE_ACTIVATION;

        /**
         * 根据给定的激活上下文返回 {@link ImportPhase}
         *
         * @param activationContext 激活上下文
         */
        static ImportPhase get(ConfigDataActivationContext activationContext) {
            if (activationContext != null && activationContext.getProfiles() != null) {
                return AFTER_PROFILE_ACTIVATION;
            }
            return BEFORE_PROFILE_ACTIVATION;
        }
    }

    /**
     * 遍历 contributor 树的迭代器。
     */
    private final class ContributorIterator implements Iterator<ConfigDataEnvironmentContributor> {
        private ImportPhase phase;
        private Iterator<ConfigDataEnvironmentContributor> children;
        private Iterator<ConfigDataEnvironmentContributor> current;
        private ConfigDataEnvironmentContributor next;

        private ContributorIterator() {
            this.phase = ImportPhase.AFTER_PROFILE_ACTIVATION;
            this.children = getChildren(this.phase).iterator();
            this.current = Collections.emptyIterator();
        }

        @Override
        public boolean hasNext() {
            return fetchIfNecessary() != null;
        }

        @Override
        public ConfigDataEnvironmentContributor next() {
            ConfigDataEnvironmentContributor next = fetchIfNecessary();
            if (next == null) {
                throw new NoSuchElementException();
            }
            this.next = null;
            return next;
        }

        private ConfigDataEnvironmentContributor fetchIfNecessary() {
            if (this.next != null) {
                return this.next;
            }
            if (this.current.hasNext()) {
                this.next = this.current.next();
                return this.next;
            }
            if (this.children.hasNext()) {
                this.current = this.children.next().iterator();
                return fetchIfNecessary();
            }
            if (this.phase == ImportPhase.AFTER_PROFILE_ACTIVATION) {
                this.phase = ImportPhase.BEFORE_PROFILE_ACTIVATION;
                this.children = getChildren(this.phase).iterator();
                return fetchIfNecessary();
            }
            if (this.phase == ImportPhase.BEFORE_PROFILE_ACTIVATION) {
                this.phase = null;
                this.next = ConfigDataEnvironmentContributor.this;
                return this.next;
            }
            return null;
        }
    }
}
