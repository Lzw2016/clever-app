package org.clever.core.env;

/**
 * 表示当前应用程序运行环境的接口。
 * 为应用程序环境的两个关键方面建模：profiles和properties。
 * 与属性访问相关的方法通过{@link PropertyResolver} superinterface
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:15 <br/>
 *
 * @see PropertyResolver
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 */
public interface Environment extends PropertyResolver {
    /**
     * 返回为此环境显式激活的配置文件集。
     * 概要文件用于创建要有条件注册的bean定义的逻辑分组，例如基于部署环境。
     * 可以通过将"clever.profiles.active"设置为系统属性或调用{@link ConfigurableEnvironment#setActiveProfiles(String...)}来激活配置文件。
     * 如果没有明确指定为活动的配置文件，则将自动激活任何{@linkplain #getDefaultProfiles() 默认}配置文件
     *
     * @see #getDefaultProfiles
     * @see ConfigurableEnvironment#setActiveProfiles
     * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
     */
    String[] getActiveProfiles();

    /**
     * 当没有明确设置活动配置文件时，返回默认情况下处于活动状态的配置文件集
     *
     * @see #getActiveProfiles
     * @see ConfigurableEnvironment#setDefaultProfiles
     * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
     */
    String[] getDefaultProfiles();

    /**
     * 返回{@linkplain #getActiveProfiles() active profiles}否与给定{@link Profiles}匹配
     */
    boolean acceptsProfiles(Profiles profiles);
}
