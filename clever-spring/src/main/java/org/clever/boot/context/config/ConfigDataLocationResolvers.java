package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.util.Instantiator;
import org.clever.core.env.Environment;
import org.clever.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 通过 {@code clever.factories} 加载的 {@link ConfigDataLocationResolver} 实例的集合。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:26 <br/>
 */
class ConfigDataLocationResolvers {
    private final List<ConfigDataLocationResolver<?>> resolvers;

    /**
     * 创建一个新的 {@link ConfigDataLocationResolvers} 实例
     *
     * @param binder         一个从初始 {@link Environment} 提供值的活页夹
     * @param resourceLoader {@link ResourceLoader} 加载资源位置
     */
    ConfigDataLocationResolvers(Binder binder, ResourceLoader resourceLoader) {
        this(binder, resourceLoader, Collections.singletonList(StandardConfigDataLocationResolver.class.getName()));
    }

    /**
     * 创建一个新的 {@link ConfigDataLocationResolvers} 实例
     *
     * @param binder         {@link Binder} providing values from the initial {@link Environment}
     * @param resourceLoader {@link ResourceLoader} to load resource locations
     * @param names          the {@link ConfigDataLocationResolver} class names
     */
    ConfigDataLocationResolvers(Binder binder, ResourceLoader resourceLoader, List<String> names) {
        Instantiator<ConfigDataLocationResolver<?>> instantiator = new Instantiator<>(ConfigDataLocationResolver.class,
                (availableParameters) -> {
                    availableParameters.add(Logger.class, LoggerFactory::getLogger);
                    availableParameters.add(Binder.class, binder);
                    availableParameters.add(ResourceLoader.class, resourceLoader);
                });
        this.resolvers = reorder(instantiator.instantiate(resourceLoader.getClassLoader(), names));
    }

    private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver<?>> resolvers) {
        List<ConfigDataLocationResolver<?>> reordered = new ArrayList<>(resolvers.size());
        StandardConfigDataLocationResolver resourceResolver = null;
        for (ConfigDataLocationResolver<?> resolver : resolvers) {
            if (resolver instanceof StandardConfigDataLocationResolver) {
                resourceResolver = (StandardConfigDataLocationResolver) resolver;
            } else {
                reordered.add(resolver);
            }
        }
        if (resourceResolver != null) {
            reordered.add(resourceResolver);
        }
        return Collections.unmodifiableList(reordered);
    }

    List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) {
        if (location == null) {
            return Collections.emptyList();
        }
        for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
            if (resolver.isResolvable(context, location)) {
                return resolve(resolver, context, location, profiles);
            }
        }
        throw new UnsupportedConfigDataLocationException(location);
    }

    private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolver<?> resolver,
                                                     ConfigDataLocationResolverContext context,
                                                     ConfigDataLocation location,
                                                     Profiles profiles) {
        List<ConfigDataResolutionResult> resolved = resolve(location, false, () -> resolver.resolve(context, location));
        if (profiles == null) {
            return resolved;
        }
        List<ConfigDataResolutionResult> profileSpecific = resolve(
                location, true, () -> resolver.resolveProfileSpecific(context, location, profiles)
        );
        return merge(resolved, profileSpecific);
    }

    private List<ConfigDataResolutionResult> resolve(ConfigDataLocation location, boolean profileSpecific, Supplier<List<? extends ConfigDataResource>> resolveAction) {
        List<ConfigDataResource> resources = nonNullList(resolveAction.get());
        List<ConfigDataResolutionResult> resolved = new ArrayList<>(resources.size());
        for (ConfigDataResource resource : resources) {
            resolved.add(new ConfigDataResolutionResult(location, resource, profileSpecific));
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> nonNullList(List<? extends T> list) {
        return (list != null) ? (List<T>) list : Collections.emptyList();
    }

    private <T> List<T> merge(List<T> list1, List<T> list2) {
        List<T> merged = new ArrayList<>(list1.size() + list2.size());
        merged.addAll(list1);
        merged.addAll(list2);
        return merged;
    }

    /**
     * 返回此对象管理的解析器
     *
     * @return 解析器
     */
    List<ConfigDataLocationResolver<?>> getResolvers() {
        return this.resolvers;
    }
}
