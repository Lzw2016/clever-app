package org.clever.boot.context.config;

import org.clever.boot.DefaultPropertiesPropertySource;
import org.clever.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
import org.clever.boot.context.properties.bind.BindException;
import org.clever.boot.context.properties.bind.Bindable;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.context.properties.bind.PlaceholdersResolver;
import org.clever.boot.context.properties.source.ConfigurationPropertySource;
import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.Environment;
import org.clever.core.env.MutablePropertySources;
import org.clever.core.env.PropertySource;
import org.clever.core.io.ResourceLoader;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 包装可用于导入和应用 {@link ConfigData} 的 {@link ConfigurableEnvironment}。
 * 通过包装 {@link Environment} 中的属性源并添加初始位置集，配置 {@link ConfigDataEnvironmentContributors} 的初始集。
 * <p>
 * 初始位置可以通过 {@link #LOCATION_PROPERTY}、{@value #ADDITIONAL_LOCATION_PROPERTY} 和 {@value #IMPORT_PROPERTY} 属性来影响。
 * 如果未设置显式属性，则将使用 {@link #DEFAULT_SEARCH_LOCATIONS}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:12 <br/>
 */
class ConfigDataEnvironment {
    /**
     * 使用的属性将覆盖导入的位置。
     */
    static final String LOCATION_PROPERTY = "clever.config.location";
    /**
     * 用于提供要导入的其他位置的属性。
     */
    static final String ADDITIONAL_LOCATION_PROPERTY = "clever.config.additional-location";
    /**
     * 用于提供要导入的其他位置的属性。
     */
    static final String IMPORT_PROPERTY = "clever.config.import";
    /**
     * 属性，用于确定在引发 {@code ConfigDataNotFoundAction} 时要采取的操作。
     *
     * @see ConfigDataNotFoundAction
     */
    static final String ON_NOT_FOUND_PROPERTY = "clever.config.on-not-found";
    /**
     * 如果未找到 {@link #LOCATION_PROPERTY}，则使用默认搜索位置。
     */
    static final ConfigDataLocation[] DEFAULT_SEARCH_LOCATIONS;

    static {
        List<ConfigDataLocation> locations = new ArrayList<>();
        locations.add(ConfigDataLocation.of("optional:classpath:/;optional:classpath:/config/"));
        locations.add(ConfigDataLocation.of("optional:file:./;optional:file:./config/;optional:file:./config/*/"));
        DEFAULT_SEARCH_LOCATIONS = locations.toArray(new ConfigDataLocation[0]);
    }

    private static final ConfigDataLocation[] EMPTY_LOCATIONS = new ConfigDataLocation[0];
    private static final Bindable<ConfigDataLocation[]> CONFIG_DATA_LOCATION_ARRAY = Bindable.of(ConfigDataLocation[].class);
    private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);
    private static final BinderOption[] ALLOW_INACTIVE_BINDING = {};
    private static final BinderOption[] DENY_INACTIVE_BINDING = {BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE};

    private final Logger logger;
    private final ConfigDataNotFoundAction notFoundAction;
    private final ConfigurableEnvironment environment;
    private final ConfigDataLocationResolvers resolvers;
    private final Collection<String> additionalProfiles;
    private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;
    private final ConfigDataLoaders loaders;
    private final ConfigDataEnvironmentContributors contributors;

    /**
     * 创建新的 {@link ConfigDataEnvironment}
     *
     * @param environment               {@link Environment}.
     * @param resourceLoader            {@link ResourceLoader} 加载资源位置
     * @param additionalProfiles        要激活的任何其他配置文件
     * @param environmentUpdateListener 可选的 {@link ConfigDataEnvironmentUpdateListener}，可用于跟踪 {@link Environment} 更新
     */
    ConfigDataEnvironment(ConfigurableEnvironment environment,
                          ResourceLoader resourceLoader,
                          Collection<String> additionalProfiles,
                          ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
        Binder binder = Binder.get(environment);
        UseLegacyConfigProcessingException.throwIfRequested(binder);
        this.logger = LoggerFactory.getLogger(getClass());
        this.notFoundAction = binder.bind(ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class).orElse(ConfigDataNotFoundAction.FAIL);
        this.environment = environment;
        this.resolvers = createConfigDataLocationResolvers(binder, resourceLoader);
        this.additionalProfiles = additionalProfiles;
        this.environmentUpdateListener = (environmentUpdateListener != null) ? environmentUpdateListener : ConfigDataEnvironmentUpdateListener.NONE;
        this.loaders = new ConfigDataLoaders(resourceLoader.getClassLoader());
        this.contributors = createContributors(binder);
    }

    protected ConfigDataLocationResolvers createConfigDataLocationResolvers(Binder binder, ResourceLoader resourceLoader) {
        return new ConfigDataLocationResolvers(binder, resourceLoader);
    }

    private ConfigDataEnvironmentContributors createContributors(Binder binder) {
        this.logger.trace("Building config data environment contributors");
        MutablePropertySources propertySources = this.environment.getPropertySources();
        List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(propertySources.size() + 10);
        PropertySource<?> defaultPropertySource = null;
        for (PropertySource<?> propertySource : propertySources) {
            if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
                defaultPropertySource = propertySource;
            } else {
                this.logger.trace(String.format(
                        "Creating wrapped config data contributor for '%s'", propertySource.getName()
                ));
                contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource));
            }
        }
        contributors.addAll(getInitialImportContributors(binder));
        if (defaultPropertySource != null) {
            this.logger.trace("Creating wrapped config data contributor for default property source");
            contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource));
        }
        return createContributors(contributors);
    }

    protected ConfigDataEnvironmentContributors createContributors(List<ConfigDataEnvironmentContributor> contributors) {
        return new ConfigDataEnvironmentContributors(contributors);
    }

    ConfigDataEnvironmentContributors getContributors() {
        return this.contributors;
    }

    private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
        List<ConfigDataEnvironmentContributor> initialContributors = new ArrayList<>();
        addInitialImportContributors(initialContributors, bindLocations(binder, IMPORT_PROPERTY, EMPTY_LOCATIONS));
        addInitialImportContributors(initialContributors, bindLocations(binder, ADDITIONAL_LOCATION_PROPERTY, EMPTY_LOCATIONS));
        addInitialImportContributors(initialContributors, bindLocations(binder, LOCATION_PROPERTY, DEFAULT_SEARCH_LOCATIONS));
        return initialContributors;
    }

    private ConfigDataLocation[] bindLocations(Binder binder, String propertyName, ConfigDataLocation[] other) {
        return binder.bind(propertyName, CONFIG_DATA_LOCATION_ARRAY).orElse(other);
    }

    private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors, ConfigDataLocation[] locations) {
        for (int i = locations.length - 1; i >= 0; i--) {
            initialContributors.add(createInitialImportContributor(locations[i]));
        }
    }

    private ConfigDataEnvironmentContributor createInitialImportContributor(ConfigDataLocation location) {
        this.logger.trace(String.format("Adding initial config data import from location '%s'", location));
        return ConfigDataEnvironmentContributor.ofInitialImport(location);
    }

    /**
     * 处理所有贡献，并将任何新导入的房地产资源应用 {@link Environment}.
     */
    void processAndApply() {
        ConfigDataImporter importer = new ConfigDataImporter(this.notFoundAction, this.resolvers, this.loaders);
        registerBootstrapBinder(this.contributors, null, DENY_INACTIVE_BINDING);
        ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
        ConfigDataActivationContext activationContext = createActivationContext(
                contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE)
        );
        contributors = processWithoutProfiles(contributors, importer, activationContext);
        activationContext = withProfiles(contributors, activationContext);
        contributors = processWithProfiles(contributors, importer, activationContext);
        applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(), importer.getOptionalLocations());
    }

    private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors, ConfigDataImporter importer) {
        this.logger.trace("Processing initial config data environment contributors without activation context");
        contributors = contributors.withProcessedImports(importer, null);
        registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
        return contributors;
    }

    private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
        this.logger.trace("Creating config data activation context from initial contributions");
        try {
            return new ConfigDataActivationContext(this.environment, initialBinder);
        } catch (BindException ex) {
            if (ex.getCause() instanceof InactiveConfigDataAccessException) {
                throw (InactiveConfigDataAccessException) ex.getCause();
            }
            throw ex;
        }
    }

    private ConfigDataEnvironmentContributors processWithoutProfiles(ConfigDataEnvironmentContributors contributors,
                                                                     ConfigDataImporter importer,
                                                                     ConfigDataActivationContext activationContext) {
        this.logger.trace("Processing config data environment contributors with initial activation context");
        contributors = contributors.withProcessedImports(importer, activationContext);
        registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
        return contributors;
    }

    private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
        this.logger.trace("Deducing profiles from current config data environment contributors");
        Binder binder = contributors.getBinder(
                activationContext,
                (contributor) -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
                BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE
        );
        try {
            Set<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
            additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
            Profiles profiles = new Profiles(this.environment, binder, additionalProfiles);
            return activationContext.withProfiles(profiles);
        } catch (BindException ex) {
            if (ex.getCause() instanceof InactiveConfigDataAccessException) {
                throw (InactiveConfigDataAccessException) ex.getCause();
            }
            throw ex;
        }
    }

    private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
        PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
                contributors, activationContext, null, true
        );
        Set<String> result = new LinkedHashSet<>();
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
            if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
                Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
                binder.bind(Profiles.INCLUDE_PROFILES, STRING_LIST).ifBound((includes) -> {
                    if (!contributor.isActive(activationContext)) {
                        InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES);
                        InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES.append("[0]"));
                    }
                    result.addAll(includes);
                });
            }
        }
        return result;
    }

    private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
                                                                  ConfigDataImporter importer,
                                                                  ConfigDataActivationContext activationContext) {
        this.logger.trace("Processing config data environment contributors with profile activation context");
        contributors = contributors.withProcessedImports(importer, activationContext);
        registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
        return contributors;
    }

    private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext, BinderOption... binderOptions) {
    }

    private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
                                    ConfigDataActivationContext activationContext,
                                    Set<ConfigDataLocation> loadedLocations,
                                    Set<ConfigDataLocation> optionalLocations) {
        checkForInvalidProperties(contributors);
        checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
        MutablePropertySources propertySources = this.environment.getPropertySources();
        applyContributor(contributors, activationContext, propertySources);
        DefaultPropertiesPropertySource.moveToEnd(propertySources);
        Profiles profiles = activationContext.getProfiles();
        this.logger.trace(String.format("Setting default profiles: %s", profiles.getDefault()));
        this.environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
        this.logger.trace(String.format("Setting active profiles: %s", profiles.getActive()));
        this.environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
        this.environmentUpdateListener.onSetProfiles(profiles);
    }

    private void applyContributor(ConfigDataEnvironmentContributors contributors,
                                  ConfigDataActivationContext activationContext,
                                  MutablePropertySources propertySources) {
        this.logger.trace("Applying config data environment contributions");
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            PropertySource<?> propertySource = contributor.getPropertySource();
            if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
                if (!contributor.isActive(activationContext)) {
                    this.logger.trace(String.format("Skipping inactive property source '%s'", propertySource.getName()));
                } else {
                    this.logger.trace(String.format("Adding imported property source '%s'", propertySource.getName()));
                    propertySources.addLast(propertySource);
                    this.environmentUpdateListener.onPropertySourceAdded(
                            propertySource, contributor.getLocation(), contributor.getResource()
                    );
                }
            }
        }
    }

    private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
        }
    }

    private void checkMandatoryLocations(ConfigDataEnvironmentContributors contributors,
                                         ConfigDataActivationContext activationContext,
                                         Set<ConfigDataLocation> loadedLocations,
                                         Set<ConfigDataLocation> optionalLocations) {
        Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>();
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            if (contributor.isActive(activationContext)) {
                mandatoryLocations.addAll(getMandatoryImports(contributor));
            }
        }
        for (ConfigDataEnvironmentContributor contributor : contributors) {
            if (contributor.getLocation() != null) {
                mandatoryLocations.remove(contributor.getLocation());
            }
        }
        mandatoryLocations.removeAll(loadedLocations);
        mandatoryLocations.removeAll(optionalLocations);
        if (!mandatoryLocations.isEmpty()) {
            for (ConfigDataLocation mandatoryLocation : mandatoryLocations) {
                this.notFoundAction.handle(this.logger, new ConfigDataLocationNotFoundException(mandatoryLocation));
            }
        }
    }

    private Set<ConfigDataLocation> getMandatoryImports(ConfigDataEnvironmentContributor contributor) {
        List<ConfigDataLocation> imports = contributor.getImports();
        Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>(imports.size());
        for (ConfigDataLocation location : imports) {
            if (!location.isOptional()) {
                mandatoryLocations.add(location);
            }
        }
        return mandatoryLocations;
    }
}
