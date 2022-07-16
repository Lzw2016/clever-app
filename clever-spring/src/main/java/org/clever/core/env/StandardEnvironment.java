package org.clever.core.env;

/**
 * 适用于"standard"(即非web)应用程序的{@link Environment}实现。
 * <p>除了{@link ConfigurableEnvironment}的常规功能(如属性解析和与概要文件相关的操作)之外，
 * 此实现还配置了两个默认属性源，将按以下顺序进行搜索：
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() 系统属性}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
 * </ul>
 * <p>也就是说，如果键“xyz”既存在于JVM系统属性中，也存在于当前进程的环境变量集中，
 * 则系统属性中的键“xyz”的值将从对环境的调用{@code environment.getProperty("xyz")}中返回。
 * 默认情况下选择此顺序，因为系统属性是每个JVM的，而环境变量在给定系统上的许多JVM中可能是相同的。
 * 赋予系统属性优先级允许在每个JVM的基础上重写环境变量
 *
 * <p>可以删除、重新排序或替换这些默认属性源；
 * 可以使用{@link #getPropertySources()}中提供的{@link MutablePropertySources}实例添加其他属性源。
 * 有关用法示例，请参阅{@link ConfigurableEnvironment} Javadoc。
 *
 * <p>请参阅{@link SystemEnvironmentPropertySource} javadoc，
 * 了解有关在shell环境(例如Bash)中对属性名称进行特殊处理的详细信息，这些环境不允许在变量名称中使用句点字符。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 21:11 <br/>
 *
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 */
public class StandardEnvironment extends AbstractEnvironment {
    /**
     * 系统环境属性源名称: {@value}.
     */
    public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";
    /**
     * JVM系统属性属性源名称: {@value}.
     */
    public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";

    /**
     * 使用默认的{@link MutablePropertySources}实例创建新的{@code StandardEnvironment}实例
     */
    public StandardEnvironment() {
    }

    /**
     * 使用特定的{@link MutablePropertySources}实例创建新的{@code StandardEnvironment}实例
     *
     * @param propertySources 要使用的属性源
     */
    protected StandardEnvironment(MutablePropertySources propertySources) {
        super(propertySources);
    }

    /**
     * 使用适用于任何标准Java环境的属性源自定义属性源集：
     * <ul>
     * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
     * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
     * </ul>
     * <p>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}中的属性优先于{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}中的属性
     *
     * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
     * @see #getSystemProperties()
     * @see #getSystemEnvironment()
     */
    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        propertySources.addLast(new PropertiesPropertySource(
                SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                getSystemProperties()
        ));
        propertySources.addLast(new SystemEnvironmentPropertySource(
                SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                getSystemEnvironment()
        ));
    }
}
