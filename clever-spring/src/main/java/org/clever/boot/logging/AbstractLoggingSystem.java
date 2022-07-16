package org.clever.boot.logging;

import org.clever.core.env.Environment;
import org.clever.core.io.ClassPathResource;
import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;
import org.clever.util.SystemPropertyUtils;

import java.util.*;

/**
 * {@link LoggingSystem} 实现的抽象基类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:44 <br/>
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {
    protected static final Comparator<LoggerConfiguration> CONFIGURATION_COMPARATOR = new LoggerConfigurationComparator(ROOT_LOGGER_NAME);

    private final ClassLoader classLoader;

    public AbstractLoggingSystem(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void beforeInitialize() {
    }

    @Override
    public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
        if (StringUtils.hasLength(configLocation)) {
            initializeWithSpecificConfig(initializationContext, configLocation, logFile);
            return;
        }
        initializeWithConventions(initializationContext, logFile);
    }

    private void initializeWithSpecificConfig(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
        configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
        loadConfiguration(initializationContext, configLocation, logFile);
    }

    private void initializeWithConventions(LoggingInitializationContext initializationContext, LogFile logFile) {
        String config = getSelfInitializationConfig();
        if (config != null && logFile == null) {
            // self initialization has occurred, reinitialize in case of property changes
            reinitialize(initializationContext);
            return;
        }
        if (config == null) {
            config = getSpringInitializationConfig();
        }
        if (config != null) {
            loadConfiguration(initializationContext, config, logFile);
            return;
        }
        loadDefaults(initializationContext, logFile);
    }

    /**
     * 返回已应用的任何自初始化配置。
     * 默认情况下，此方法检查 {@link #getStandardConfigLocations()} 并假定已应用任何存在的文件。
     *
     * @return 自初始化配置或 null
     */
    protected String getSelfInitializationConfig() {
        return findConfig(getStandardConfigLocations());
    }

    /**
     * 返回应该应用的任何特定的初始化配置。
     * 默认情况下，此方法检查 {@link #getSpringConfigLocations()}。
     *
     * @return 初始化配置或 null
     */
    protected String getSpringInitializationConfig() {
        return findConfig(getSpringConfigLocations());
    }

    private String findConfig(String[] locations) {
        for (String location : locations) {
            ClassPathResource resource = new ClassPathResource(location, this.classLoader);
            if (resource.exists()) {
                return "classpath:" + location;
            }
        }
        return null;
    }

    /**
     * 返回此系统的标准配置位置
     *
     * @return 标准配置位置
     * @see #getSelfInitializationConfig()
     */
    protected abstract String[] getStandardConfigLocations();

    /**
     * 返回此系统的弹簧配置位置。
     * 默认情况下，此方法返回一组基于 {@link #getStandardConfigLocations()} 的位置。
     *
     * @return 配置位置
     * @see #getSpringInitializationConfig()
     */
    protected String[] getSpringConfigLocations() {
        String[] locations = getStandardConfigLocations();
        for (int i = 0; i < locations.length; i++) {
            String extension = StringUtils.getFilenameExtension(locations[i]);
            locations[i] = locations[i].substring(0, locations[i].length() - extension.length() - 1) + "-clever." + extension;
        }
        return locations;
    }

    /**
     * 为日志系统加载合理的默认值。
     *
     * @param initializationContext the logging initialization context
     * @param logFile               要加载的文件，如果不写入日志文件，则为 null
     */
    protected abstract void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile);

    /**
     * 加载特定配置。
     *
     * @param initializationContext 日志记录初始化上下文
     * @param location              要加载的配置的位置（从不 null）
     * @param logFile               要加载的文件，如果不写入日志文件，则为 null
     */
    protected abstract void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile);

    /**
     * 如果需要，重新初始化日志系统。
     * 在使用 {@link #getSelfInitializationConfig()} 且日志文件未更改时调用。
     * 可用于重新加载配置（例如获取额外的系统属性）。
     *
     * @param initializationContext 日志记录初始化上下文
     */
    protected void reinitialize(LoggingInitializationContext initializationContext) {
    }

    protected final ClassLoader getClassLoader() {
        return this.classLoader;
    }

    protected final String getPackagedConfigFile(String fileName) {
        String defaultPath = ClassUtils.getPackageName(getClass());
        defaultPath = defaultPath.replace('.', '/');
        defaultPath = defaultPath + "/" + fileName;
        defaultPath = "classpath:" + defaultPath;
        return defaultPath;
    }

    protected final void applySystemProperties(Environment environment, LogFile logFile) {
        new LoggingSystemProperties(environment).apply(logFile);
    }

    /**
     * 维护原生级别和 {@link LogLevel} 之间的映射
     *
     * @param <T> 本机级别类型
     */
    protected static class LogLevels<T> {
        private final Map<LogLevel, T> systemToNative;
        private final Map<T, LogLevel> nativeToSystem;

        public LogLevels() {
            this.systemToNative = new EnumMap<>(LogLevel.class);
            this.nativeToSystem = new HashMap<>();
        }

        public void map(LogLevel system, T nativeLevel) {
            this.systemToNative.putIfAbsent(system, nativeLevel);
            this.nativeToSystem.putIfAbsent(nativeLevel, system);
        }

        public LogLevel convertNativeToSystem(T level) {
            return this.nativeToSystem.get(level);
        }

        public T convertSystemToNative(LogLevel level) {
            return this.systemToNative.get(level);
        }

        public Set<LogLevel> getSupported() {
            return new LinkedHashSet<>(this.nativeToSystem.values());
        }
    }
}

