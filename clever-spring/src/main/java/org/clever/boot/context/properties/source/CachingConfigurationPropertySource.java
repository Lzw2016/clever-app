package org.clever.boot.context.properties.source;

/**
 * 用于指示{@link ConfigurationPropertySource}支持{@link ConfigurationPropertyCaching}的接口。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:53 <br/>
 */
interface CachingConfigurationPropertySource {
    /**
     * 返回此源的{@link ConfigurationPropertyCaching}。
     *
     * @return 源缓存
     */
    ConfigurationPropertyCaching getCaching();

    /**
     * 查找给定源的{@link ConfigurationPropertyCaching}。
     *
     * @param source 配置属性源
     * @return 如果源不支持缓存，则为{@link ConfigurationPropertyCaching}实例或null。
     */
    static ConfigurationPropertyCaching find(ConfigurationPropertySource source) {
        if (source instanceof CachingConfigurationPropertySource) {
            return ((CachingConfigurationPropertySource) source).getCaching();
        }
        return null;
    }
}
