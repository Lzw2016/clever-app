package org.clever.core.env;

/**
 * {@link PropertyResolver}实现，根据底层的一组{@link PropertySources}解析属性值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:46 <br/>
 *
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {
    private final PropertySources propertySources;

    /**
     * 针对给定的属性源创建新的解析器
     *
     * @param propertySources 要使用的PropertySource对象集
     */
    public PropertySourcesPropertyResolver(PropertySources propertySources) {
        this.propertySources = propertySources;
    }

    @Override
    public boolean containsProperty(String key) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (propertySource.containsProperty(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class, true);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetValueType) {
        return getProperty(key, targetValueType, true);
    }

    @Override
    protected String getPropertyAsRawString(String key) {
        return getProperty(key, String.class, false);
    }

    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Searching for key '" + key + "' in PropertySource '" + propertySource.getName() + "'");
                }
                Object value = propertySource.getProperty(key);
                if (value != null) {
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = resolveNestedPlaceholders((String) value);
                    }
                    logKeyFound(key, propertySource, value);
                    return convertValueIfNecessary(value, targetValueType);
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Could not find key '" + key + "' in any property source");
        }
        return null;
    }

    /**
     * 将给定的键记录在给定的{@link PropertySource}中，生成给定的值。
     *
     * @param key            找到的密钥
     * @param propertySource 在中找到密钥的PropertySource
     * @param value          对应的值
     */
    protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
                    "' with value of type " + value.getClass().getSimpleName()
            );
        }
    }
}