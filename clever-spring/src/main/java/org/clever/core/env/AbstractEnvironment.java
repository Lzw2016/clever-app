package org.clever.core.env;

import org.clever.core.convert.support.ConfigurableConversionService;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;
import java.util.*;

/**
 * 用于环境实现的抽象基类。支持保留默认profile名称的概念，
 * 并允许通过{@link #ACTIVE_PROFILES_PROPERTY_NAME}和{@link #DEFAULT_PROFILES_PROPERTY_NAME}属性指定活动和默认profile
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 20:39 <br/>
 *
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment {
    /**
     * 指示忽略系统环境变量的系统属性，即从不尝试通过{@link System#getenv()}检索此类变量。
     * 默认值为“false”
     *
     * @see #suppressGetenvAccess()
     */
    public static final String IGNORE_GETENV_PROPERTY_NAME = "clever.getenv.ignore";
    /**
     * 要设置以指定活动profile的属性名称：{@value}。值可以用逗号分隔。
     * 请注意，某些shell环境（如Bash）不允许在变量名中使用句点字符。
     * 假设{@link SystemEnvironmentPropertySource}正在使用，
     * 则可以将此属性指定为环境变量{@code SPRING_PROFILES_ACTIVE}
     *
     * @see ConfigurableEnvironment#setActiveProfiles
     */
    public static final String ACTIVE_PROFILES_PROPERTY_NAME = "clever.profiles.active";
    /**
     * 要设置为指定默认活动配置文件的属性名称: {@value}。值可以用逗号分隔。
     * 请注意，某些shell环境(如Bash)不允许在变量名中使用句点字符。
     * 假设{@link SystemEnvironmentPropertySource}正在使用，
     * 则可以将此属性指定为环境变量{@code SPRING_PROFILES_DEFAULT}
     *
     * @see ConfigurableEnvironment#setDefaultProfiles
     */
    public static final String DEFAULT_PROFILES_PROPERTY_NAME = "clever.profiles.default";
    /**
     * 保留默认profile名称：{@value}。
     * 如果没有显式设置默认profile名称，也没有显式设置活动profile名称，
     * 则默认情况下将自动激活此profile
     *
     * @see #getReservedDefaultProfiles
     * @see ConfigurableEnvironment#setDefaultProfiles
     * @see ConfigurableEnvironment#setActiveProfiles
     * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
     * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
     */
    protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Set<String> activeProfiles = new LinkedHashSet<>();
    private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());
    private final MutablePropertySources propertySources;
    private final ConfigurablePropertyResolver propertyResolver;

    /**
     * 创建一个新的{@code Environment}实例，
     * 在构造期间调用{@link #customizePropertySources(MutablePropertySources)}，
     * 以允许子类根据需要贡献或操作{@link PropertySource}实例。
     *
     * @see #customizePropertySources(MutablePropertySources)
     */
    public AbstractEnvironment() {
        this(new MutablePropertySources());
    }

    /**
     * 使用特定的{@link MutablePropertySources}实例创建一个新的{@code Environment}实例，
     * 在构造期间调用{@link #customizePropertySources(MutablePropertySources)}，
     * 以允许子类根据需要贡献或操作{@link PropertySource}实例
     *
     * @param propertySources 要使用的属性源
     * @see #customizePropertySources(MutablePropertySources)
     */
    protected AbstractEnvironment(MutablePropertySources propertySources) {
        this.propertySources = propertySources;
        this.propertyResolver = createPropertyResolver(propertySources);
        customizePropertySources(propertySources);
    }

    /**
     * 用于创建环境使用的{@link ConfigurablePropertyResolver}实例的工厂方法
     *
     * @see #getPropertyResolver()
     */
    protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
        return new PropertySourcesPropertyResolver(propertySources);
    }

    /**
     * 返回{@link Environment}正在使用的{@link ConfigurablePropertyResolver}
     *
     * @see #createPropertyResolver(MutablePropertySources)
     */
    protected final ConfigurablePropertyResolver getPropertyResolver() {
        return this.propertyResolver;
    }

    /**
     * 自定义此环境在调用{@link #getProperty(String)}和相关方法期间要搜索的{@link PropertySource}对象集。
     *
     * <p>鼓励重写此方法的子类使用{@link MutablePropertySources#addLast(PropertySource)}添加属性源。
     * 以便进一步的子类可以调用{@code super.customizePropertySources()}具有可预测结果。例如：
     * <pre>{@code
     * public class Level1Environment extends AbstractEnvironment {
     *     @Override
     *     protected void customizePropertySources(MutablePropertySources propertySources) {
     *         super.customizePropertySources(propertySources); // no-op from base class
     *         propertySources.addLast(new PropertySourceA(...));
     *         propertySources.addLast(new PropertySourceB(...));
     *     }
     * }
     *
     * public class Level2Environment extends Level1Environment {
     *     @Override
     *     protected void customizePropertySources(MutablePropertySources propertySources) {
     *         super.customizePropertySources(propertySources); // add all from superclass
     *         propertySources.addLast(new PropertySourceC(...));
     *         propertySources.addLast(new PropertySourceD(...));
     *     }
     * }
     * }</pre>
     *
     * <p>在这种安排中，将按照源A、B、C、D的顺序解析属性。也就是说，属性源“A”优先于属性源“D”。
     * 如果{@code Level2Environment}子类希望赋予属性源C和D比A和B更高的优先级，
     * 那么它可以简单地调用{@code super.customizePropertySources}在添加自己的资源之后而不是之前：
     * <pre>{@code
     * public class Level2Environment extends Level1Environment {
     *     @Override
     *     protected void customizePropertySources(MutablePropertySources propertySources) {
     *         propertySources.addLast(new PropertySourceC(...));
     *         propertySources.addLast(new PropertySourceD(...));
     *         super.customizePropertySources(propertySources); // 从超类添加全部
     *     }
     * }
     * }</pre>
     *
     * <p>现在，搜索顺序为C、D、A、B。
     *
     * <p>除了这些建议之外，子类还可以使用由{@link MutablePropertySources}公开的任何
     * {@code add*}、{@code remove}或{@code replace}方法，以便创建所需的属性源的精确排列。
     *
     * <p>基本实现不注册任何属性源。
     *
     * <p>请注意，任何可配置环境的客户端都可以通过{@link #getPropertySources()}访问器进一步自定义属性源。例如：
     * <pre>{@code
     * ConfigurableEnvironment env = new StandardEnvironment();
     * env.getPropertySources().addLast(new PropertySourceX(...));
     * }</pre>
     *
     * <h2>关于实例变量访问的警告</h2>
     * <p>不应从该方法中访问在子类中声明并具有默认初始值的实例变量。
     * 由于Java对象创建生命周期的限制，当{@link #AbstractEnvironment()}构造函数调用此回调时，
     * 尚未分配任何初始值，这可能会导致{@code NullPointerException}或其他问题。
     * 如果需要访问实例变量的默认值，请将此方法保留为no-op，并直接在子类构造函数中执行属性源操作和实例变量访问。
     * 请注意，为实例变量赋值没有问题；它只是试图读取必须避免的默认值。
     *
     * @see MutablePropertySources
     * @see PropertySourcesPropertyResolver
     */
    protected void customizePropertySources(MutablePropertySources propertySources) {
    }

    /**
     * 返回保留的默认profile名称集。此实现返回{@value #RESERVED_DEFAULT_PROFILE_NAME}。
     * 子类可以重写以自定义保留名称集
     *
     * @see #RESERVED_DEFAULT_PROFILE_NAME
     * @see #doGetDefaultProfiles()
     */
    protected Set<String> getReservedDefaultProfiles() {
        return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
    }

    //---------------------------------------------------------------------
    // Implementation of ConfigurableEnvironment interface
    //---------------------------------------------------------------------

    @Override
    public String[] getActiveProfiles() {
        return StringUtils.toStringArray(doGetActiveProfiles());
    }

    /**
     * 返回通过{@link #setActiveProfiles}显式设置的活动profiles，
     * 或者如果当前活动profiles为空，
     * 请检查是否存在{@link #doGetActiveProfilesProperty()}，
     * 并将其值分配给活动profiles
     *
     * @see #getActiveProfiles()
     * @see #doGetActiveProfilesProperty()
     */
    protected Set<String> doGetActiveProfiles() {
        synchronized (this.activeProfiles) {
            if (this.activeProfiles.isEmpty()) {
                String profiles = doGetActiveProfilesProperty();
                if (StringUtils.hasText(profiles)) {
                    setActiveProfiles(StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(profiles)));
                }
            }
            return this.activeProfiles;
        }
    }

    /**
     * 返回活动profile的属性值
     *
     * @see #ACTIVE_PROFILES_PROPERTY_NAME
     */
    protected String doGetActiveProfilesProperty() {
        return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
    }

    @Override
    public void setActiveProfiles(String... profiles) {
        Assert.notNull(profiles, "Profile array must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Activating profiles " + Arrays.asList(profiles));
        }
        synchronized (this.activeProfiles) {
            this.activeProfiles.clear();
            for (String profile : profiles) {
                validateProfile(profile);
                this.activeProfiles.add(profile);
            }
        }
    }

    @Override
    public void addActiveProfile(String profile) {
        if (logger.isDebugEnabled()) {
            logger.debug("Activating profile '" + profile + "'");
        }
        validateProfile(profile);
        doGetActiveProfiles();
        synchronized (this.activeProfiles) {
            this.activeProfiles.add(profile);
        }
    }

    @Override
    public String[] getDefaultProfiles() {
        return StringUtils.toStringArray(doGetDefaultProfiles());
    }

    /**
     * 返回通过{@link #setDefaultProfiles(String...)}显式设置的默认profile集或者，
     * 如果当前的默认profiles仅包含{@linkplain #getReservedDefaultProfiles() 保留的默认profile}，
     * 则检查是否存在{@link #doGetActiveProfilesProperty()}，并将其值（如果有）分配给默认profile集
     *
     * @see #AbstractEnvironment()
     * @see #getDefaultProfiles()
     * @see #getReservedDefaultProfiles()
     * @see #doGetDefaultProfilesProperty()
     */
    protected Set<String> doGetDefaultProfiles() {
        synchronized (this.defaultProfiles) {
            if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
                String profiles = doGetDefaultProfilesProperty();
                if (StringUtils.hasText(profiles)) {
                    setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(profiles)));
                }
            }
            return this.defaultProfiles;
        }
    }

    /**
     * 返回默认profile的属性值
     *
     * @see #DEFAULT_PROFILES_PROPERTY_NAME
     */
    protected String doGetDefaultProfilesProperty() {
        return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
    }

    /**
     * 如果没有其他profile通过{@link #setActiveProfiles}显式激活，则指定默认情况下要激活的profiles。
     * 调用此方法将删除覆盖在构建环境期间可能添加的任何保留默认profile。
     *
     * @see #AbstractEnvironment()
     * @see #getReservedDefaultProfiles()
     */
    @Override
    public void setDefaultProfiles(String... profiles) {
        Assert.notNull(profiles, "Profile array must not be null");
        synchronized (this.defaultProfiles) {
            this.defaultProfiles.clear();
            for (String profile : profiles) {
                validateProfile(profile);
                this.defaultProfiles.add(profile);
            }
        }
    }

    @Override
    public boolean acceptsProfiles(Profiles profiles) {
        Assert.notNull(profiles, "Profiles must not be null");
        return profiles.matches(this::isProfileActive);
    }

    /**
     * 返回给定profile是否处于活动状态，或者如果活动profile为空，则返回默认情况下该profile是否应处于活动状态
     *
     * @throws IllegalArgumentException per {@link #validateProfile(String)}
     */
    protected boolean isProfileActive(String profile) {
        validateProfile(profile);
        Set<String> currentActiveProfiles = doGetActiveProfiles();
        return (currentActiveProfiles.contains(profile) || (currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
    }

    /**
     * 验证给定的profile，在添加到活动或默认profile集之前在内部调用
     * <p>子类可以重写以对概要文件语法施加进一步的限制
     *
     * @throws IllegalArgumentException 如果profile为null、空、仅空白或以profileNOT运算符(!)开头
     * @see #acceptsProfiles
     * @see #addActiveProfile
     * @see #setDefaultProfiles
     */
    protected void validateProfile(String profile) {
        if (!StringUtils.hasText(profile)) {
            throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
        }
        if (profile.charAt(0) == '!') {
            throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
        }
    }

    @Override
    public MutablePropertySources getPropertySources() {
        return this.propertySources;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getSystemProperties() {
        try {
            return (Map) System.getProperties();
        } catch (AccessControlException ex) {
            return (Map) new ReadOnlySystemAttributesMap() {
                @Override
                protected String getSystemAttribute(String attributeName) {
                    try {
                        return System.getProperty(attributeName);
                    } catch (AccessControlException ex) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Caught AccessControlException when accessing system property '" +
                                    attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage()
                            );
                        }
                        return null;
                    }
                }
            };
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getSystemEnvironment() {
        if (suppressGetenvAccess()) {
            return Collections.emptyMap();
        }
        try {
            return (Map) System.getenv();
        } catch (AccessControlException ex) {
            return (Map) new ReadOnlySystemAttributesMap() {
                @Override
                protected String getSystemAttribute(String attributeName) {
                    try {
                        return System.getenv(attributeName);
                    } catch (AccessControlException ex) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Caught AccessControlException when accessing system environment variable '" +
                                    attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage()
                            );
                        }
                        return null;
                    }
                }
            };
        }
    }

    /**
     * 确定是否抑制{@link System#getenv()}/{@link System#getenv(String)}访问，用于{@link #getSystemEnvironment()}。
     * 如果此方法返回true，则将使用空的虚拟Map来代替常规的系统环境Map，甚至从不尝试调用getenv，从而避免安全管理器警告。
     *
     * @see #IGNORE_GETENV_PROPERTY_NAME
     */
    protected boolean suppressGetenvAccess() {
        return false;
        // return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
    }

    @Override
    public void merge(ConfigurableEnvironment parent) {
        for (PropertySource<?> ps : parent.getPropertySources()) {
            if (!this.propertySources.contains(ps.getName())) {
                this.propertySources.addLast(ps);
            }
        }
        String[] parentActiveProfiles = parent.getActiveProfiles();
        if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
            synchronized (this.activeProfiles) {
                Collections.addAll(this.activeProfiles, parentActiveProfiles);
            }
        }
        String[] parentDefaultProfiles = parent.getDefaultProfiles();
        if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
            synchronized (this.defaultProfiles) {
                this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
                Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
            }
        }
    }

    //---------------------------------------------------------------------
    // Implementation of ConfigurablePropertyResolver interface
    //---------------------------------------------------------------------

    @Override
    public ConfigurableConversionService getConversionService() {
        return this.propertyResolver.getConversionService();
    }

    @Override
    public void setConversionService(ConfigurableConversionService conversionService) {
        this.propertyResolver.setConversionService(conversionService);
    }

    @Override
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
    }

    @Override
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
    }

    @Override
    public void setValueSeparator(String valueSeparator) {
        this.propertyResolver.setValueSeparator(valueSeparator);
    }

    @Override
    public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
        this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
    }

    @Override
    public void setRequiredProperties(String... requiredProperties) {
        this.propertyResolver.setRequiredProperties(requiredProperties);
    }

    @Override
    public void validateRequiredProperties() throws MissingRequiredPropertiesException {
        this.propertyResolver.validateRequiredProperties();
    }

    //---------------------------------------------------------------------
    // Implementation of PropertyResolver interface
    //---------------------------------------------------------------------

    @Override
    public boolean containsProperty(String key) {
        return this.propertyResolver.containsProperty(key);
    }

    @Override
    public String getProperty(String key) {
        return this.propertyResolver.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.propertyResolver.getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return this.propertyResolver.getProperty(key, targetType);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return this.propertyResolver.getProperty(key, targetType, defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key);
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        return this.propertyResolver.getRequiredProperty(key, targetType);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return this.propertyResolver.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return this.propertyResolver.resolveRequiredPlaceholders(text);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " {activeProfiles=" + this.activeProfiles +
                ", defaultProfiles=" + this.defaultProfiles +
                ", propertySources=" + this.propertySources + "}";
    }
}
