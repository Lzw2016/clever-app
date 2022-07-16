package org.clever.boot.context.config;

import org.clever.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.clever.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.clever.boot.context.properties.bind.*;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;
import org.clever.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 用于处理导入的 {@link ConfigDataEnvironmentContributors} 的不可变树结构。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:19 <br/>
 */
class ConfigDataEnvironmentContributors implements Iterable<ConfigDataEnvironmentContributor> {
    private static final Predicate<ConfigDataEnvironmentContributor> NO_CONTRIBUTOR_FILTER = (contributor) -> true;

    private final Logger logger;
    private final ConfigDataEnvironmentContributor root;

    /**
     * 创建一个新的 {@link ConfigDataEnvironmentContributors} 实例。
     *
     * @param contributors 最初的贡献者集合
     */
    ConfigDataEnvironmentContributors(List<ConfigDataEnvironmentContributor> contributors) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.root = ConfigDataEnvironmentContributor.of(contributors);
    }

    private ConfigDataEnvironmentContributors(Logger logger, ConfigDataEnvironmentContributor root) {
        this.logger = logger;
        this.root = root;
    }

    /**
     * 处理来自所有活跃贡献者的导入并返回一个新的 {@link ConfigDataEnvironmentContributors} 实例。
     *
     * @param importer          用于导入 {@link ConfigData} 的导入器
     * @param activationContext 当前激活上下文；如果尚未创建上下文，则为 null
     * @return 已处理所有相关导入的 {@link ConfigDataEnvironmentContributors} 实例
     */
    ConfigDataEnvironmentContributors withProcessedImports(ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
        ImportPhase importPhase = ImportPhase.get(activationContext);
        this.logger.trace(String.format(
                "Processing imports for phase %s. %s",
                importPhase,
                (activationContext != null) ? activationContext : "no activation context"
        ));
        ConfigDataEnvironmentContributors result = this;
        int processed = 0;
        while (true) {
            ConfigDataEnvironmentContributor contributor = getNextToProcess(result, activationContext, importPhase);
            if (contributor == null) {
                this.logger.trace(String.format("Processed imports for of %d contributors", processed));
                return result;
            }
            if (contributor.getKind() == Kind.UNBOUND_IMPORT) {
                ConfigDataEnvironmentContributor bound = contributor.withBoundProperties(result, activationContext);
                result = new ConfigDataEnvironmentContributors(this.logger, result.getRoot().withReplacement(contributor, bound));
                continue;
            }
            ConfigDataLocationResolverContext locationResolverContext = new ContributorConfigDataLocationResolverContext(
                    result, contributor, activationContext
            );
            ConfigDataLoaderContext loaderContext = new ContributorDataLoaderContext(this);
            List<ConfigDataLocation> imports = contributor.getImports();
            this.logger.trace(String.format("Processing imports %s", imports));
            Map<ConfigDataResolutionResult, ConfigData> imported = importer.resolveAndLoad(
                    activationContext, locationResolverContext, loaderContext, imports
            );
            if (this.logger.isTraceEnabled()) {
                this.logger.trace(String.valueOf(getImportedMessage(imported.keySet())));
            }
            ConfigDataEnvironmentContributor contributorAndChildren = contributor.withChildren(importPhase, asContributors(imported));
            result = new ConfigDataEnvironmentContributors(this.logger, result.getRoot().withReplacement(contributor, contributorAndChildren));
            processed++;
        }
    }

    private CharSequence getImportedMessage(Set<ConfigDataResolutionResult> results) {
        if (results.isEmpty()) {
            return "Nothing imported";
        }
        StringBuilder message = new StringBuilder();
        message.append("Imported ")
                .append(results.size())
                .append(" resource")
                .append((results.size() != 1) ? "s " : " ");
        message.append(results.stream().map(ConfigDataResolutionResult::getResource).collect(Collectors.toList()));
        return message;
    }

    private ConfigDataEnvironmentContributor getNextToProcess(ConfigDataEnvironmentContributors contributors,
                                                              ConfigDataActivationContext activationContext,
                                                              ImportPhase importPhase) {
        for (ConfigDataEnvironmentContributor contributor : contributors.getRoot()) {
            if (contributor.getKind() == Kind.UNBOUND_IMPORT || isActiveWithUnprocessedImports(activationContext, importPhase, contributor)) {
                return contributor;
            }
        }
        return null;
    }

    private boolean isActiveWithUnprocessedImports(ConfigDataActivationContext activationContext,
                                                   ImportPhase importPhase,
                                                   ConfigDataEnvironmentContributor contributor) {
        return contributor.isActive(activationContext) && contributor.hasUnprocessedImports(importPhase);
    }

    private List<ConfigDataEnvironmentContributor> asContributors(Map<ConfigDataResolutionResult, ConfigData> imported) {
        List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(imported.size() * 5);
        imported.forEach((resolutionResult, data) -> {
            ConfigDataLocation location = resolutionResult.getLocation();
            ConfigDataResource resource = resolutionResult.getResource();
            boolean profileSpecific = resolutionResult.isProfileSpecific();
            if (data.getPropertySources().isEmpty()) {
                contributors.add(ConfigDataEnvironmentContributor.ofEmptyLocation(location, profileSpecific));
            } else {
                for (int i = data.getPropertySources().size() - 1; i >= 0; i--) {
                    contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(
                            location, resource, profileSpecific, data, i
                    ));
                }
            }
        });
        return Collections.unmodifiableList(contributors);
    }

    /**
     * 返回 root contributor
     */
    ConfigDataEnvironmentContributor getRoot() {
        return this.root;
    }

    /**
     * 返回由 contributors 支持的 {@link Binder}
     *
     * @param activationContext 激活上下文
     * @param options           要应用的BinderOption
     * @return binder 实例
     */
    Binder getBinder(ConfigDataActivationContext activationContext, BinderOption... options) {
        return getBinder(activationContext, NO_CONTRIBUTOR_FILTER, options);
    }

    /**
     * 返回由 contributors 支持的 {@link Binder}
     *
     * @param activationContext 激活上下文
     * @param filter            用于限制贡献者的过滤器
     * @param options           要应用的BinderOption
     * @return binder 实例
     */
    Binder getBinder(ConfigDataActivationContext activationContext,
                     Predicate<ConfigDataEnvironmentContributor> filter,
                     BinderOption... options) {
        return getBinder(activationContext, filter, asBinderOptionsSet(options));
    }

    private Set<BinderOption> asBinderOptionsSet(BinderOption... options) {
        return ObjectUtils.isEmpty(options) ?
                EnumSet.noneOf(BinderOption.class)
                : EnumSet.copyOf(Arrays.asList(options));
    }

    private Binder getBinder(ConfigDataActivationContext activationContext,
                             Predicate<ConfigDataEnvironmentContributor> filter,
                             Set<BinderOption> options) {
        boolean failOnInactiveSource = options.contains(BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
        Iterable<ConfigurationPropertySource> sources = () -> getBinderSources(
                filter.and((contributor) -> failOnInactiveSource || contributor.isActive(activationContext))
        );
        PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
                this.root,
                activationContext,
                null,
                failOnInactiveSource
        );
        BindHandler bindHandler = !failOnInactiveSource ? null : new InactiveSourceChecker(activationContext);
        return new Binder(sources, placeholdersResolver, null, null, bindHandler);
    }

    private Iterator<ConfigurationPropertySource> getBinderSources(Predicate<ConfigDataEnvironmentContributor> filter) {
        return this.root.stream()
                .filter(this::hasConfigurationPropertySource)
                .filter(filter)
                .map(ConfigDataEnvironmentContributor::getConfigurationPropertySource)
                .iterator();
    }

    private boolean hasConfigurationPropertySource(ConfigDataEnvironmentContributor contributor) {
        return contributor.getConfigurationPropertySource() != null;
    }

    @Override
    public Iterator<ConfigDataEnvironmentContributor> iterator() {
        return this.root.iterator();
    }

    /**
     * {@link ConfigDataLocationResolverContext} for a contributor.
     */
    private static class ContributorDataLoaderContext implements ConfigDataLoaderContext {
        private final ConfigDataEnvironmentContributors contributors;

        ContributorDataLoaderContext(ConfigDataEnvironmentContributors contributors) {
            this.contributors = contributors;
        }
    }

    /**
     * {@link ConfigDataLocationResolverContext} for a contributor.
     */
    private static class ContributorConfigDataLocationResolverContext implements ConfigDataLocationResolverContext {
        private final ConfigDataEnvironmentContributors contributors;
        private final ConfigDataEnvironmentContributor contributor;
        private final ConfigDataActivationContext activationContext;
        private volatile Binder binder;

        ContributorConfigDataLocationResolverContext(ConfigDataEnvironmentContributors contributors,
                                                     ConfigDataEnvironmentContributor contributor,
                                                     ConfigDataActivationContext activationContext) {
            this.contributors = contributors;
            this.contributor = contributor;
            this.activationContext = activationContext;
        }

        @Override
        public Binder getBinder() {
            Binder binder = this.binder;
            if (binder == null) {
                binder = this.contributors.getBinder(this.activationContext);
                this.binder = binder;
            }
            return binder;
        }

        @Override
        public ConfigDataResource getParent() {
            return this.contributor.getResource();
        }
    }

    private class InactiveSourceChecker implements BindHandler {
        private final ConfigDataActivationContext activationContext;

        InactiveSourceChecker(ConfigDataActivationContext activationContext) {
            this.activationContext = activationContext;
        }

        @Override
        public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
            for (ConfigDataEnvironmentContributor contributor : ConfigDataEnvironmentContributors.this) {
                if (!contributor.isActive(this.activationContext)) {
                    InactiveConfigDataAccessException.throwIfPropertyFound(contributor, name);
                }
            }
            return result;
        }
    }

    /**
     * 可用于Binder的选项 {@link ConfigDataEnvironmentContributors#getBinder(ConfigDataActivationContext, BinderOption...)}.
     */
    enum BinderOption {
        /**
         * 如果非活动贡献者包含绑定值，则抛出异常。
         */
        FAIL_ON_BIND_TO_INACTIVE_SOURCE;
    }
}
