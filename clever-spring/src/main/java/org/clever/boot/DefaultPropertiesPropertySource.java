package org.clever.boot;

import org.clever.core.env.*;
import org.clever.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@link MapPropertySource} 包含直接贡献给 {@code SpringApplication} 的默认属性。
 * 按照惯例，{@link DefaultPropertiesPropertySource} 始终是 {@link Environment} 中的最后一个属性源。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:42 <br/>
 */
public class DefaultPropertiesPropertySource extends MapPropertySource {
    /**
     * “default properties”属性源的名称。
     */
    public static final String NAME = "defaultProperties";

    /**
     * 创建新的 {@link DefaultPropertiesPropertySource} 使用给定的 {@code Map} 源
     *
     * @param source 源 Map
     */
    public DefaultPropertiesPropertySource(Map<String, Object> source) {
        super(NAME, source);
    }

    /**
     * 如果给定源名为“defaultProperties”，则返回true。
     *
     * @param propertySource 要检查的属性源
     * @return 如果名称匹配，则为true
     */
    public static boolean hasMatchingName(PropertySource<?> propertySource) {
        return (propertySource != null) && propertySource.getName().equals(NAME);
    }

    /**
     * 如果提供的源不为空，则创建一个新的 {@link DefaultPropertiesPropertySource} 实例。
     *
     * @param source {@code Map} 源
     * @param action 用于消费 {@link DefaultPropertiesPropertySource} 的操作
     */
    public static void ifNotEmpty(Map<String, Object> source, Consumer<DefaultPropertiesPropertySource> action) {
        if (!CollectionUtils.isEmpty(source) && action != null) {
            action.accept(new DefaultPropertiesPropertySource(source));
        }
    }

    /**
     * 添加新的 {@link DefaultPropertiesPropertySource} 或与现有资源合并。
     *
     * @param source  {@code Map} 源
     * @param sources 现有的 sources
     */
    public static void addOrMerge(Map<String, Object> source, MutablePropertySources sources) {
        if (!CollectionUtils.isEmpty(source)) {
            Map<String, Object> resultingSource = new HashMap<>();
            DefaultPropertiesPropertySource propertySource = new DefaultPropertiesPropertySource(resultingSource);
            if (sources.contains(NAME)) {
                mergeIfPossible(source, sources, resultingSource);
                sources.replace(NAME, propertySource);
            } else {
                resultingSource.putAll(source);
                sources.addLast(propertySource);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeIfPossible(Map<String, Object> source, MutablePropertySources sources, Map<String, Object> resultingSource) {
        PropertySource<?> existingSource = sources.get(NAME);
        if (existingSource != null) {
            Object underlyingSource = existingSource.getSource();
            if (underlyingSource instanceof Map) {
                resultingSource.putAll((Map<String, Object>) underlyingSource);
            }
            resultingSource.putAll(source);
        }
    }

    /**
     * 移动“defaultProperties”属性源，使其成为给定 {@link ConfigurableEnvironment} 中的最后一个源。
     *
     * @param environment 要更新的 environment
     */
    public static void moveToEnd(ConfigurableEnvironment environment) {
        moveToEnd(environment.getPropertySources());
    }

    /**
     * 移动“defaultProperties”属性源，使其成为给定 {@link MutablePropertySources} 中的最后一个源。
     *
     * @param propertySources 要更新的属性源
     */
    public static void moveToEnd(MutablePropertySources propertySources) {
        PropertySource<?> propertySource = propertySources.remove(NAME);
        if (propertySource != null) {
            propertySources.addLast(propertySource);
        }
    }
}
