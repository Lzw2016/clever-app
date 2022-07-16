package org.clever.boot.logging;

import org.clever.core.env.ConfigurableEnvironment;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 日志系统的通用抽象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:31 <br/>
 */
public abstract class LoggingSystem {
    /**
     * 可用于指示要使用的 {@link LoggingSystem} 的系统属性
     */
    public static final String SYSTEM_PROPERTY = LoggingSystem.class.getName();
    /**
     * {@link #SYSTEM_PROPERTY} 的值可用于指示不应使用 {@link LoggingSystem}。
     */
    public static final String NONE = "none";
    /**
     * 用于根记录器的名称。 LoggingSystem 实现应确保这是用于表示根记录器的名称，无论底层实现如何。
     */
    public static final String ROOT_LOGGER_NAME = "ROOT";
    private static final LoggingSystemFactory SYSTEM_FACTORY = LoggingSystemFactory.fromSpringFactories();

    /**
     * 返回应该应用的 {@link LoggingSystemProperties}。
     *
     * @param environment 用于获取值的 {@link ConfigurableEnvironment}
     * @return 要应用的 {@link LoggingSystemProperties}
     */
    public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
        return new LoggingSystemProperties(environment);
    }

    /**
     * 将记录系统重置为限制输出。
     * 该方法可以在 {@link #initialize(LoggingInitializationContext, String, LogFile)} 之前调用，以减少日志噪音，直到系统完全初始化。
     */
    public abstract void beforeInitialize();

    /**
     * 完全初始化日志系统
     *
     * @param initializationContext 日志初始化上下文 日志配置位置，如果需要默认初始化，则为 null
     * @param logFile               应写入的日志输出文件或仅用于控制台输出的 null
     */
    public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
    }

    /**
     * 清理日志系统。默认实现什么也不做。子类应覆盖此方法以执行任何特定于日志记录系统的清理。
     */
    public void cleanUp() {
    }

    /**
     * 返回一个 {@link Runnable}，它可以在 JVM 退出时处理此日志系统的关闭。
     * 默认实现返回null，表示不需要关闭。
     *
     * @return 关闭处理程序，或 null
     */
    public Runnable getShutdownHandler() {
        return null;
    }

    /**
     * 返回日志系统实际支持的一组 {@link LogLevel LogLevels}
     */
    public Set<LogLevel> getSupportedLogLevels() {
        return EnumSet.allOf(LogLevel.class);
    }

    /**
     * 设置给定记录器的记录级别。
     *
     * @param loggerName 要设置的记录器的名称（null 可用于根记录器）。
     * @param level      日志级别（null 可用于删除记录器的任何自定义级别并改用默认配置）
     */
    public void setLogLevel(String loggerName, LogLevel level) {
        throw new UnsupportedOperationException("Unable to set log level");
    }

    /**
     * 返回所有 {@link LoggingSystem} 记录器的当前配置的集合。
     *
     * @return 当前配置
     */
    public List<LoggerConfiguration> getLoggerConfigurations() {
        throw new UnsupportedOperationException("Unable to get logger configurations");
    }

    /**
     * 返回 {@link LoggingSystem} 记录器的当前配置。
     *
     * @param loggerName 记录器的名称
     * @return 当前配置
     */
    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        throw new UnsupportedOperationException("Unable to get logger configuration");
    }

    /**
     * 检测并返回正在使用的日志系统。支持 Logback 和 Java 日志记录。
     *
     * @param classLoader 类加载器
     * @return 日志系统
     */
    public static LoggingSystem get(ClassLoader classLoader) {
        String loggingSystemClassName = System.getProperty(SYSTEM_PROPERTY);
        if (StringUtils.hasLength(loggingSystemClassName)) {
            if (NONE.equals(loggingSystemClassName)) {
                return new NoOpLoggingSystem();
            }
            return get(classLoader, loggingSystemClassName);
        }
        LoggingSystem loggingSystem = SYSTEM_FACTORY.getLoggingSystem(classLoader);
        Assert.state(loggingSystem != null, "No suitable logging system located");
        return loggingSystem;
    }

    private static LoggingSystem get(ClassLoader classLoader, String loggingSystemClassName) {
        try {
            Class<?> systemClass = ClassUtils.forName(loggingSystemClassName, classLoader);
            Constructor<?> constructor = systemClass.getDeclaredConstructor(ClassLoader.class);
            constructor.setAccessible(true);
            return (LoggingSystem) constructor.newInstance(classLoader);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * {@link LoggingSystem} 什么都不做
     */
    static class NoOpLoggingSystem extends LoggingSystem {
        @Override
        public void beforeInitialize() {
        }

        @Override
        public void setLogLevel(String loggerName, LogLevel level) {
        }

        @Override
        public List<LoggerConfiguration> getLoggerConfigurations() {
            return Collections.emptyList();
        }

        @Override
        public LoggerConfiguration getLoggerConfiguration(String loggerName) {
            return null;
        }
    }
}
