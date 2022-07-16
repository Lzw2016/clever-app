package org.clever.boot.context.properties.source;

import org.clever.core.env.*;
import org.clever.core.env.PropertySource.StubPropertySource;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.stream.Stream;

/**
 * 提供访问 {@link ConfigurationPropertySource ConfigurationPropertySources}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:55 <br/>
 */
public final class ConfigurationPropertySources {
    /**
     * {@link PropertySource} {@link #attach(Environment) 适配器} 的名称
     */
    private static final String ATTACHED_PROPERTY_SOURCE_NAME = "configurationProperties";

    private ConfigurationPropertySources() {
    }

    /**
     * 创建一个新的{@link PropertyResolver}，该{@link PropertyResolver}根据基础{@link PropertySources}集解析属性值。
     * 提供了一种具有{@link ConfigurationPropertySource}意识且优化的{@link PropertySourcesPropertyResolver}替代方案。
     *
     * @param propertySources 要使用的{@link PropertySource}对象
     * @return {@link ConfigurablePropertyResolver} 实现
     */
    public static ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
        return new ConfigurationPropertySourcesPropertyResolver(propertySources);
    }

    /**
     * 确定特定{@link PropertySource}是否为{@link #attach(Environment) 连接}到{@link Environment}的{@link ConfigurationPropertySource}。
     *
     * @param propertySource 要测试的属性源
     * @return 如果这是所附的{@link ConfigurationPropertySource}，则为true
     */
    public static boolean isAttachedConfigurationPropertySource(PropertySource<?> propertySource) {
        return ATTACHED_PROPERTY_SOURCE_NAME.equals(propertySource.getName());
    }

    /**
     * 将{@link ConfigurationPropertySource}支持附加到指定的{@link Environment}。
     * 将环境管理的每个{@link PropertySource}调整为{@link ConfigurationPropertySource}，
     * 并允许经典的{@link PropertySourcesPropertyResolver}调用使用 {@link ConfigurationPropertyName 配置属性名称} 进行解析。
     * <p>
     * 附加的解析器将动态跟踪从底层{@link Environment}属性源添加或删除的任何内容。
     *
     * @param environment 源环境 (必须是的实例 {@link ConfigurableEnvironment})
     * @see #get(Environment)
     */
    public static void attach(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
        PropertySource<?> attached = getAttached(sources);
        // noinspection PointlessNullCheck
        if (attached == null || !isUsingSources(attached, sources)) {
            attached = new ConfigurationPropertySourcesPropertySource(
                    ATTACHED_PROPERTY_SOURCE_NAME, new SpringConfigurationPropertySources(sources)
            );
        }
        sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
        sources.addFirst(attached);
    }

    private static boolean isUsingSources(PropertySource<?> attached, MutablePropertySources sources) {
        return attached instanceof ConfigurationPropertySourcesPropertySource && ((SpringConfigurationPropertySources) attached.getSource()).isUsingSources(sources);
    }

    static PropertySource<?> getAttached(MutablePropertySources sources) {
        return (sources != null) ? sources.get(ATTACHED_PROPERTY_SOURCE_NAME) : null;
    }

    /**
     * 返回以前{@link #attach(Environment) 附加} 到 {@link Environment} 的一组实例。
     *
     * @param environment 源环境(必须是的实例 {@link ConfigurableEnvironment})
     * @return 一组可移植的配置属性源
     * @throws IllegalStateException 如果没有配置，则已附加属性源
     */
    public static Iterable<ConfigurationPropertySource> get(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
        ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
        if (attached == null) {
            return from(sources);
        }
        return attached.getSource();
    }

    /**
     * 返回{@link Iterable}，其中包含从给定弹簧{@link PropertySource}改编的单个新{@link ConfigurationPropertySource}。
     *
     * @param source 要适应的属性源
     * @return 一个{@link Iterable}，包含一个新修改的 {@link SpringConfigurationPropertySource}
     */
    public static Iterable<ConfigurationPropertySource> from(PropertySource<?> source) {
        return Collections.singleton(ConfigurationPropertySource.from(source));
    }

    /**
     * 返回{@link Iterable}，其中包含从给定{@link PropertySource PropertySources}改编的新 {@link ConfigurationPropertySource} 实例。
     * <p>
     * 此方法将展平任何嵌套的属性源，并过滤所有{@link StubPropertySource 存根属性源}。
     * 由迭代器返回的源中的更改标识的对底层源的更新将被自动跟踪。底层源应该是线程安全的，
     * 例如可变属性源 {@link MutablePropertySources}
     *
     * @param sources 要适应的属性源
     * @return 包含新调整的 {@link SpringConfigurationPropertySource} 实例的 {@link Iterable}
     */
    public static Iterable<ConfigurationPropertySource> from(Iterable<PropertySource<?>> sources) {
        return new SpringConfigurationPropertySources(sources);
    }

    private static Stream<PropertySource<?>> streamPropertySources(PropertySources sources) {
        return sources.stream()
                .flatMap(ConfigurationPropertySources::flatten)
                .filter(ConfigurationPropertySources::isIncluded);
    }

    private static Stream<PropertySource<?>> flatten(PropertySource<?> source) {
        if (source.getSource() instanceof ConfigurableEnvironment) {
            return streamPropertySources(((ConfigurableEnvironment) source.getSource()).getPropertySources());
        }
        return Stream.of(source);
    }

    private static boolean isIncluded(PropertySource<?> source) {
        return !(source instanceof StubPropertySource) && !(source instanceof ConfigurationPropertySourcesPropertySource);
    }
}
