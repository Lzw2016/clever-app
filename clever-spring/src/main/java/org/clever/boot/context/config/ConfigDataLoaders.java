package org.clever.boot.context.config;

import org.clever.boot.util.Instantiator;
import org.clever.core.ResolvableType;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通过 {@code clever.factories} 加载的 {@link ConfigDataLoader} 实例的集合。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:38 <br/>
 */
public class ConfigDataLoaders {
    private final Logger logger;
    private final List<ConfigDataLoader<?>> loaders;
    private final List<Class<?>> resourceTypes;

    /**
     * 创建一个新的 {@link ConfigDataLoaders} 实例。
     *
     * @param classLoader 加载时使用的类加载器
     */
    ConfigDataLoaders(ClassLoader classLoader) {
        this(classLoader, Collections.singletonList(StandardConfigDataLoader.class.getName()));
    }

    /**
     * 创建一个新的 {@link ConfigDataLoaders} 实例。
     *
     * @param classLoader 加载时使用的类加载器
     * @param names       {@link ConfigDataLoader} 类名实例化
     */
    ConfigDataLoaders(ClassLoader classLoader, List<String> names) {
        this.logger = LoggerFactory.getLogger(getClass());
        Instantiator<ConfigDataLoader<?>> instantiator = new Instantiator<>(ConfigDataLoader.class,
                (availableParameters) -> {
                    // availableParameters
                    availableParameters.add(Logger.class, LoggerFactory::getLogger);
                });
        this.loaders = instantiator.instantiate(classLoader, names);
        this.resourceTypes = getResourceTypes(this.loaders);
    }

    private List<Class<?>> getResourceTypes(List<ConfigDataLoader<?>> loaders) {
        List<Class<?>> resourceTypes = new ArrayList<>(loaders.size());
        for (ConfigDataLoader<?> loader : loaders) {
            resourceTypes.add(getResourceType(loader));
        }
        return Collections.unmodifiableList(resourceTypes);
    }

    private Class<?> getResourceType(ConfigDataLoader<?> loader) {
        return ResolvableType.forClass(loader.getClass()).as(ConfigDataLoader.class).resolveGeneric();
    }

    /**
     * 使用第一个适当的 {@link ConfigDataLoader} 加载 {@link ConfigData}
     *
     * @param <R>      资源类型
     * @param context  加载器上下文
     * @param resource 要加载的资源
     * @return 加载的 {@link ConfigData}
     * @throws IOException IO 错误
     */
    <R extends ConfigDataResource> ConfigData load(ConfigDataLoaderContext context, R resource) throws IOException {
        ConfigDataLoader<R> loader = getLoader(context, resource);
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Loading " + resource + " using loader " + loader.getClass().getName());
        }
        return loader.load(context, resource);
    }

    @SuppressWarnings("unchecked")
    private <R extends ConfigDataResource> ConfigDataLoader<R> getLoader(ConfigDataLoaderContext context, R resource) {
        ConfigDataLoader<R> result = null;
        for (int i = 0; i < this.loaders.size(); i++) {
            ConfigDataLoader<?> candidate = this.loaders.get(i);
            if (this.resourceTypes.get(i).isInstance(resource)) {
                ConfigDataLoader<R> loader = (ConfigDataLoader<R>) candidate;
                if (loader.isLoadable(context, resource)) {
                    if (result != null) {
                        throw new IllegalStateException("Multiple loaders found for resource '" + resource + "' ["
                                + candidate.getClass().getName() + "," + result.getClass().getName() + "]"
                        );
                    }
                    result = loader;
                }
            }
        }
        Assert.state(result != null, () -> "No loader found for resource '" + resource + "'");
        return result;
    }
}
