package org.clever.boot.context.properties.source;

import org.clever.core.env.Environment;
import org.clever.util.Assert;

import java.time.Duration;

/**
 * 可用于控制配置属性源缓存的接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:54 <br/>
 */
public interface ConfigurationPropertyCaching {
    /**
     * 启用无限制生存时间的缓存。
     */
    void enable();

    /**
     * 禁用缓存。
     */
    void disable();

    /**
     * 设置项目可以在缓存中生存的时间量。调用此方法也将启用缓存。
     *
     * @param timeToLive 生存时间值
     */
    void setTimeToLive(Duration timeToLive);

    /**
     * 清除缓存并强制在下次访问时重新加载。
     */
    void clear();

    /**
     * 获取环境中的所有配置属性源。
     *
     * @param environment 环境
     * @return 控制环境中所有源的缓存实例
     */
    static ConfigurationPropertyCaching get(Environment environment) {
        return get(environment, null);
    }

    /**
     * 获取环境中的特定配置属性源。
     *
     * @param environment      环境
     * @param underlyingSource 必须匹配的{@link ConfigurationPropertySource#getUnderlyingSource() 基础源}
     * @return 控制匹配源的缓存实例
     */
    static ConfigurationPropertyCaching get(Environment environment, Object underlyingSource) {
        Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
        return get(sources, underlyingSource);
    }

    /**
     * 获取所有指定的配置属性源。
     *
     * @param sources 配置属性源
     * @return 控制源的缓存实例
     */
    static ConfigurationPropertyCaching get(Iterable<ConfigurationPropertySource> sources) {
        return get(sources, null);
    }

    /**
     * 获取指定配置属性源中的特定配置属性源。
     *
     * @param sources          配置属性源
     * @param underlyingSource 必须匹配的{@link ConfigurationPropertySource#getUnderlyingSource() 基础源}
     * @return 控制匹配源的缓存实例
     */
    static ConfigurationPropertyCaching get(Iterable<ConfigurationPropertySource> sources, Object underlyingSource) {
        Assert.notNull(sources, "Sources must not be null");
        if (underlyingSource == null) {
            return new ConfigurationPropertySourcesCaching(sources);
        }
        for (ConfigurationPropertySource source : sources) {
            if (source.getUnderlyingSource() == underlyingSource) {
                ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
                if (caching != null) {
                    return caching;
                }
            }
        }
        throw new IllegalStateException("Unable to find cache from configuration property sources");
    }
}
