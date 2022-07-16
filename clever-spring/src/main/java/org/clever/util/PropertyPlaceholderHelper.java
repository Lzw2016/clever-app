package org.clever.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 用于处理包含占位符值的字符串的实用工具类。
 * 占位符的形式为{@code ${name}}。
 * 使用{@code PropertyPlaceholderHelper}，这些占位符可以替换为用户提供的值。
 * 可以使用{@link Properties}实例或{@link PlaceholderResolver}提供替换值。
 * 占位符解析器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:50 <br/>
 */
public class PropertyPlaceholderHelper {
    private static final Logger logger = LoggerFactory.getLogger(PropertyPlaceholderHelper.class);
    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    private final String placeholderPrefix;
    private final String placeholderSuffix;
    private final String simplePrefix;
    private final String valueSeparator;
    private final boolean ignoreUnresolvablePlaceholders;

    /**
     * 创建使用提供的前缀和后缀的新{@code PropertyPlaceholderHelper}。将忽略无法解析的占位符
     *
     * @param placeholderPrefix 表示占位符开头的前缀
     * @param placeholderSuffix 表示占位符结尾的后缀
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * 创建使用提供的前缀和后缀的新{@code PropertyPlaceholderHelper}
     *
     * @param placeholderPrefix              表示占位符开头的前缀
     * @param placeholderSuffix              表示占位符结尾的后缀
     * @param valueSeparator                 占位符变量和关联默认值（如果有）之间的分隔字符
     * @param ignoreUnresolvablePlaceholders 指示是应忽略不可解析占位符（true）还是应导致异常（false）
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix, String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
        Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
        Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * 用提供的属性中的相应属性替换{@code ${name}}格式的所有占位符
     *
     * @param value      包含要替换的占位符的值
     * @param properties 用于替换的属性
     * @return 替换了占位符的内联提供的值
     */
    public String replacePlaceholders(String value, final Properties properties) {
        Assert.notNull(properties, "'properties' must not be null");
        return replacePlaceholders(value, properties::getProperty);
    }

    /**
     * 将{@code ${name}}格式的所有占位符替换为从提供的PropertyPlaceholderHelper返回的值。
     * {@link PlaceholderResolver}
     *
     * @param value               包含要替换的占位符的值
     * @param placeholderResolver 用于替换的占位符解析器
     * @return 替换了占位符的内联提供的值
     */
    public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
        Assert.notNull(value, "'value' must not be null");
        return parseStringValue(value, placeholderResolver, null);
    }

    protected String parseStringValue(String value, PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {
        int startIndex = value.indexOf(this.placeholderPrefix);
        if (startIndex == -1) {
            return value;
        }
        StringBuilder result = new StringBuilder(value);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex != -1) {
                String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (visitedPlaceholders == null) {
                    visitedPlaceholders = new HashSet<>(4);
                }
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException("Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
                // Now obtain the value for the fully resolved key...
                String propVal = placeholderResolver.resolvePlaceholder(placeholder);
                if (propVal == null && this.valueSeparator != null) {
                    int separatorIndex = placeholder.indexOf(this.valueSeparator);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                        propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                    result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resolved placeholder '" + placeholder + "'");
                    }
                    startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                } else if (this.ignoreUnresolvablePlaceholders) {
                    // Proceed with unprocessed value.
                    startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                } else {
                    throw new IllegalArgumentException("Could not resolve placeholder '" + placeholder + "'" + " in value \"" + value + "\"");
                }
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }
        return result.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    /**
     * 用于解析字符串中占位符的替换值的策略接口
     */
    @FunctionalInterface
    public interface PlaceholderResolver {
        /**
         * 将提供的占位符名称解析为替换值
         *
         * @param placeholderName 要解析的占位符的名称
         * @return 替换值，如果不进行替换，则为null
         */
        String resolvePlaceholder(String placeholderName);
    }
}
