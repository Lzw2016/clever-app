package org.clever.boot.context.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * 通过 {@link ConfigDataLocationResolver 解析} 和 {@link ConfigDataLoader 加载} 位置来导入 {@link ConfigData}。
 * 跟踪 {@link ConfigDataResource 资源} 以确保它们不会被多次导入。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:37 <br/>
 */
public class ConfigDataImporter {
    private final Logger logger;
    private final ConfigDataLocationResolvers resolvers;
    private final ConfigDataLoaders loaders;
    private final ConfigDataNotFoundAction notFoundAction;
    private final Set<ConfigDataResource> loaded = new HashSet<>();
    private final Set<ConfigDataLocation> loadedLocations = new HashSet<>();
    private final Set<ConfigDataLocation> optionalLocations = new HashSet<>();

    /**
     * 创建一个新的 {@link ConfigDataImporter} 实例。
     *
     * @param notFoundAction 找不到位置时采取的措施
     * @param resolvers      配置数据位置解析器
     * @param loaders        配置数据加载器
     */
    ConfigDataImporter(ConfigDataNotFoundAction notFoundAction, ConfigDataLocationResolvers resolvers, ConfigDataLoaders loaders) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.resolvers = resolvers;
        this.loaders = loaders;
        this.notFoundAction = notFoundAction;
    }

    /**
     * 解析并加载给定的位置列表，过滤之前加载的任何位置。
     *
     * @param activationContext       激活上下文
     * @param locationResolverContext 位置解析器上下文
     * @param loaderContext           加载器上下文
     * @param locations               要解决的位置
     * @return 加载位置和数据的Map
     */
    public Map<ConfigDataResolutionResult, ConfigData> resolveAndLoad(ConfigDataActivationContext activationContext,
                                                                      ConfigDataLocationResolverContext locationResolverContext,
                                                                      ConfigDataLoaderContext loaderContext,
                                                                      List<ConfigDataLocation> locations) {
        try {
            Profiles profiles = (activationContext != null) ? activationContext.getProfiles() : null;
            List<ConfigDataResolutionResult> resolved = resolve(locationResolverContext, profiles, locations);
            return load(loaderContext, resolved);
        } catch (IOException ex) {
            throw new IllegalStateException("IO error on loading imports from " + locations, ex);
        }
    }

    private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext, Profiles profiles, List<ConfigDataLocation> locations) {
        List<ConfigDataResolutionResult> resolved = new ArrayList<>(locations.size());
        for (ConfigDataLocation location : locations) {
            resolved.addAll(resolve(locationResolverContext, profiles, location));
        }
        return Collections.unmodifiableList(resolved);
    }

    private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext, Profiles profiles, ConfigDataLocation location) {
        try {
            return this.resolvers.resolve(locationResolverContext, location, profiles);
        } catch (ConfigDataNotFoundException ex) {
            handle(ex, location, null);
            return Collections.emptyList();
        }
    }

    private Map<ConfigDataResolutionResult, ConfigData> load(ConfigDataLoaderContext loaderContext, List<ConfigDataResolutionResult> candidates) throws IOException {
        Map<ConfigDataResolutionResult, ConfigData> result = new LinkedHashMap<>();
        for (int i = candidates.size() - 1; i >= 0; i--) {
            ConfigDataResolutionResult candidate = candidates.get(i);
            ConfigDataLocation location = candidate.getLocation();
            ConfigDataResource resource = candidate.getResource();
            if (resource.isOptional()) {
                this.optionalLocations.add(location);
            }
            if (this.loaded.contains(resource)) {
                this.loadedLocations.add(location);
            } else {
                try {
                    ConfigData loaded = this.loaders.load(loaderContext, resource);
                    if (loaded != null) {
                        this.loaded.add(resource);
                        this.loadedLocations.add(location);
                        result.put(candidate, loaded);
                    }
                } catch (ConfigDataNotFoundException ex) {
                    handle(ex, location, resource);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private void handle(ConfigDataNotFoundException ex, ConfigDataLocation location, ConfigDataResource resource) {
        if (ex instanceof ConfigDataResourceNotFoundException) {
            ex = ((ConfigDataResourceNotFoundException) ex).withLocation(location);
        }
        getNotFoundAction(location, resource).handle(this.logger, ex);
    }

    private ConfigDataNotFoundAction getNotFoundAction(ConfigDataLocation location, ConfigDataResource resource) {
        if (location.isOptional() || (resource != null && resource.isOptional())) {
            return ConfigDataNotFoundAction.IGNORE;
        }
        return this.notFoundAction;
    }

    Set<ConfigDataLocation> getLoadedLocations() {
        return this.loadedLocations;
    }

    Set<ConfigDataLocation> getOptionalLocations() {
        return this.optionalLocations;
    }
}
