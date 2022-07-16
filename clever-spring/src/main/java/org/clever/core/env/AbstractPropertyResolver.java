package org.clever.core.env;

import org.clever.core.convert.ConversionService;
import org.clever.core.convert.support.ConfigurableConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.PropertyPlaceholderHelper;
import org.clever.util.SystemPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用于针对任何基础源解析属性的抽象基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:47 <br/>
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile ConfigurableConversionService conversionService;
    private PropertyPlaceholderHelper nonStrictHelper;
    private PropertyPlaceholderHelper strictHelper;
    private boolean ignoreUnresolvableNestedPlaceholders = false;
    private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;
    private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;
    private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;
    private final Set<String> requiredProperties = new LinkedHashSet<>();

    @Override
    public ConfigurableConversionService getConversionService() {
        // Need to provide an independent DefaultConversionService, not the
        // shared DefaultConversionService used by PropertySourcesPropertyResolver.
        ConfigurableConversionService cs = this.conversionService;
        if (cs == null) {
            synchronized (this) {
                cs = this.conversionService;
                if (cs == null) {
                    cs = new DefaultConversionService();
                    this.conversionService = cs;
                }
            }
        }
        return cs;
    }

    @Override
    public void setConversionService(ConfigurableConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    /**
     * 设置由此解析程序替换的占位符必须以开头的前缀
     * <p>默认值为 "${".
     *
     * @see org.clever.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
     */
    @Override
    public void setPlaceholderPrefix(String placeholderPrefix) {
        Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * 设置此解析程序替换的占位符必须以其结尾的后缀。
     * <p>默认值为 "}".
     *
     * @see org.clever.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
     */
    @Override
    public void setPlaceholderSuffix(String placeholderSuffix) {
        Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * 指定此解析程序替换的占位符与其关联的默认值之间的分隔字符，如果不应将此类特殊字符作为值分隔符处理，则指定null
     * <p>默认值为 ":".
     *
     * @see org.clever.util.SystemPropertyUtils#VALUE_SEPARATOR
     */
    @Override
    public void setValueSeparator(String valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    /**
     * 设置当遇到嵌套在给定属性值中的无法解析占位符时，是否引发异常。
     * false值表示严格解析，即将引发异常。
     * true值表示应在未解析的${...}中传递无法解析的嵌套占位符类型
     * <p>默认值为 {@code false}.
     */
    @Override
    public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
        this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
    }

    @Override
    public void setRequiredProperties(String... requiredProperties) {
        Collections.addAll(this.requiredProperties, requiredProperties);
    }

    @Override
    public void validateRequiredProperties() {
        MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
        for (String key : this.requiredProperties) {
            if (this.getProperty(key) == null) {
                ex.addMissingRequiredProperty(key);
            }
        }
        if (!ex.getMissingRequiredProperties().isEmpty()) {
            throw ex;
        }
    }

    @Override
    public boolean containsProperty(String key) {
        return (getProperty(key) != null);
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null ? value : defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        T value = getProperty(key, targetType);
        return (value != null ? value : defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        String value = getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
        T value = getProperty(key, valueType);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    @Override
    public String resolvePlaceholders(String text) {
        if (this.nonStrictHelper == null) {
            this.nonStrictHelper = createPlaceholderHelper(true);
        }
        return doResolvePlaceholders(text, this.nonStrictHelper);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        if (this.strictHelper == null) {
            this.strictHelper = createPlaceholderHelper(false);
        }
        return doResolvePlaceholders(text, this.strictHelper);
    }

    /**
     * 解析给定字符串中的占位符，根据{@link #setIgnoreUnresolvableNestedPlaceholders}的值确定任何无法解析的占位符是否应引发异常或被忽略。
     * 从{@link #getProperty}及其变体调用，隐式解析嵌套占位符。
     * 相反，{@link #resolvePlaceholders}和{@link #resolveRequiredPlaceholders}不会委托给此方法，
     * 而是按照这些方法中的每个方法的指定，自行处理无法解析的占位符
     *
     * @see #setIgnoreUnresolvableNestedPlaceholders
     */
    protected String resolveNestedPlaceholders(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return (this.ignoreUnresolvableNestedPlaceholders ? resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
    }

    private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
        return new PropertyPlaceholderHelper(
                this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, ignoreUnresolvablePlaceholders
        );
    }

    private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
        return helper.replacePlaceholders(text, this::getPropertyAsRawString);
    }

    /**
     * 如有必要，将给定值转换为指定的目标类型
     *
     * @param value      原始财产价值
     * @param targetType 属性检索的指定目标类型
     * @return 转换后的值，如果不需要转换，则为原始值
     */
    @SuppressWarnings("unchecked")
    protected <T> T convertValueIfNecessary(Object value, Class<T> targetType) {
        if (targetType == null) {
            return (T) value;
        }
        ConversionService conversionServiceToUse = this.conversionService;
        if (conversionServiceToUse == null) {
            // Avoid initialization of shared DefaultConversionService if
            // no standard type conversion is needed in the first place...
            if (ClassUtils.isAssignableValue(targetType, value)) {
                return (T) value;
            }
            conversionServiceToUse = DefaultConversionService.getSharedInstance();
        }
        return conversionServiceToUse.convert(value, targetType);
    }

    /**
     * 以原始字符串的形式检索指定的属性，即不解析嵌套占位符。
     *
     * @param key 要解析的属性名称
     * @return 属性值，如果未找到，则为null
     */
    protected abstract String getPropertyAsRawString(String key);
}
