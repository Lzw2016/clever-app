package org.clever.core.env;

/**
 * 用于根据任何基础源解析属性的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 15:17 <br/>
 *
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
public interface PropertyResolver {
    /**
     * 返回给定属性键是否可用于解析，即如果给定键的值不可用{@code null}.
     */
    boolean containsProperty(String key);

    /**
     * 返回与给定键关联的属性值，如果无法解析该键，则返回null
     *
     * @param key 要解析的属性名称
     * @see #getProperty(String, String)
     * @see #getProperty(String, Class)
     * @see #getRequiredProperty(String)
     */
    String getProperty(String key);

    /**
     * 返回与给定键关联的属性值，如果无法解析该键，则返回{@code defaultValue}
     *
     * @param key          要解析的属性名称
     * @param defaultValue 如果找不到值，则返回默认值
     * @see #getRequiredProperty(String)
     * @see #getProperty(String, Class)
     */
    String getProperty(String key, String defaultValue);

    /**
     * 返回与给定键关联的属性值，如果无法解析该键，则返回{@code defaultValue}
     *
     * @param key        要解析的属性名称
     * @param targetType 属性值的预期类型
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType);

    /**
     * 返回与给定键关联的属性值，如果无法解析该键，则返回{@code defaultValue}
     *
     * @param key          要解析的属性名称
     * @param targetType   属性值的预期类型
     * @param defaultValue 如果找不到值，则返回默认值
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    /**
     * 返回与给定键关联的属性值(从不为null)
     *
     * @throws IllegalStateException 如果无法解析密钥
     * @see #getRequiredProperty(String, Class)
     */
    String getRequiredProperty(String key) throws IllegalStateException;

    /**
     * 返回与给定键关联的属性值，并将其转换为给定的targetType(从不为null).
     *
     * @throws IllegalStateException 如果给定密钥无法解析
     */
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

    /**
     * 解析${...}给定文本中的占位符，将其替换为由{@link #getProperty}解析的相应属性值。
     * 没有默认值的不可解析占位符将被忽略，并以不变的方式传递
     *
     * @param text 要解析的字符串
     * @return 解析的字符串(从不为null)
     * @throws IllegalArgumentException 如果给定文本为null
     * @see #resolveRequiredPlaceholders
     */
    String resolvePlaceholders(String text);

    /**
     * 解析${...}给定文本中的占位符，将其替换为由{@link #getProperty}解析的相应属性值。
     * 没有默认值的不可解析占位符将导致引发IllegalArgumentException
     *
     * @return 解析的字符串(从不为null)
     * @throws IllegalArgumentException 如果给定文本为null或者如果任何占位符无法解析
     */
    String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;
}
