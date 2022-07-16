package org.clever.core.env;

import java.util.Map;

/**
 * 要由大多数{@link Environment}类型实现的配置接口。
 * 提供用于设置活动profile和默认profile以及操纵基础特性源的工具。
 * 允许客户端通过{@link ConfigurablePropertyResolver}父接口设置和验证所需的属性、自定义转换服务等。
 *
 * <h4>示例：添加具有最高搜索优先级的新属性源</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>示例：删除默认系统属性属性源</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>示例：出于测试目的模拟系统环境</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:18 <br/>
 *
 * @see StandardEnvironment
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {
    /**
     * 指定此环境中活动的Profiles。
     *
     * @throws IllegalArgumentException 如果任何profile为null、空或仅为空白
     * @see #addActiveProfile
     * @see #setDefaultProfiles
     * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
     */
    void setActiveProfiles(String... profiles);

    /**
     * 将Profile添加到当前活动Profiles
     *
     * @throws IllegalArgumentException 如果profile为null、空或仅为空白
     * @see #setActiveProfiles
     */
    void addActiveProfile(String profile);

    /**
     * 设置默认的 profiles
     *
     * @throws IllegalArgumentException 如果任何profile为null、空或仅为空白
     * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
     */
    void setDefaultProfiles(String... profiles);

    /**
     * 以可变形式返回此环境的{@link PropertySources}，允许在针对此{@code Environment}对象解析属性时操作应搜索的{@link PropertySource}对象集。
     * 各种可变属性源方法(
     * 如{@link MutablePropertySources#addFirst addFirst}、
     * {@link MutablePropertySources#addLast addLast}、
     * {@link MutablePropertySources#addBefore addBefore}和
     * {@link MutablePropertySources#addAfter addAfter}
     * )
     * 允许对属性源排序进行细粒度控制。
     * 例如，这有助于确保某些用户定义的特性源的搜索优先级高于默认特性源，例如系统特性集或系统环境变量集
     *
     * @see AbstractEnvironment#customizePropertySources
     */
    MutablePropertySources getPropertySources();

    /**
     * 返回{@link System#getProperties()}值，如果当前SecurityManager允许，则返回一个映射实现，
     * 该实现将尝试使用对系统的调用{@link System#getProperty(String)}访问各个键。
     * 请注意，大多数环境实现都将此系统属性映射作为要搜索的默认属性源。
     * 因此，建议不要直接使用此方法，除非明确打算绕过其他财产来源。
     * 调用{@link Map#get(Object)}永远不会抛出IllegalAccessException；
     * 在SecurityManager禁止访问属性的情况下，将返回null，并发出一条信息级日志消息，指出异常
     */
    Map<String, Object> getSystemProperties();

    /**
     * 返回{@link System#getenv()}的值。如果当前SecurityManager允许，则返回一个映射实现，
     * 该实现将尝试使用对系统的调用{@link System#getenv(String)}访问各个键。
     * 请注意，大多数环境实现都将此系统环境映射作为要搜索的默认属性源。
     * 因此，建议不要直接使用此方法，除非明确打算绕过其他财产来源。
     * 调用{@link Map#get(Object)}永远不会抛出IllegalAccessException；
     * 在SecurityManager禁止访问属性的情况下，将返回null，并发出一条信息级日志消息，指出异常
     */
    Map<String, Object> getSystemEnvironment();

    /**
     * 将给定父环境的活动profile、默认profile和属性源附加到此（子）环境各自的集合中。
     * 对于父实例和子实例中存在的任何同名PropertySource实例，将保留子实例，并丢弃父实例。
     * 这样做的效果是允许子级重写属性源，并避免通过常见属性源类型（例如系统环境和系统属性）进行重复搜索。
     * 活动和默认profile名称也会过滤重复的profile名称，以避免混淆和冗余存储。
     * 在任何情况下，父环境都保持不变。请注意，在调用merge之后对父环境所做的任何更改都不会反映在子环境中。
     * 因此，在调用merge之前，应小心配置父属性源和profile信息
     *
     * @param parent 要与之合并的环境
     */
    void merge(ConfigurableEnvironment parent);
}
