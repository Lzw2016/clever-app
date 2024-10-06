package org.clever.spring.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.logging.*;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 用于初始化日志模块，参考 {@code LoggingApplicationListener}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/15 15:27 <br/>
 */
public class LoggingBootstrap {
    private static final ConfigurationPropertyName LOGGING_LEVEL = ConfigurationPropertyName.of("logging.level");
    private static final ConfigurationPropertyName LOGGING_GROUP = ConfigurationPropertyName.of("logging.group");
    private static final Bindable<Map<String, LogLevel>> STRING_LOGLEVEL_MAP = Bindable.mapOf(String.class, LogLevel.class);
    private static final Bindable<Map<String, List<String>>> STRING_STRINGS_MAP = Bindable.of(
        ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class).asMap()
    );
    /**
     * 包含对要加载的日志记录配置的引用的属性的名称
     */
    public static final String CONFIG_PROPERTY = "logging.config";
    private static final Map<String, List<String>> DEFAULT_GROUP_LOGGERS;

    static {
        MultiValueMap<String, String> loggers = new LinkedMultiValueMap<>();
        loggers.add("web", "org.springframework.core.codec");
        loggers.add("web", "org.springframework.http");
        loggers.add("web", "org.springframework.web");
        loggers.add("web", "org.springframework.boot.actuate.endpoint.web");
        loggers.add("web", "org.springframework.boot.web.servlet.ServletContextInitializerBeans");
        loggers.add("sql", "org.springframework.jdbc.core");
        loggers.add("sql", "org.hibernate.SQL");
        loggers.add("sql", "org.jooq.tools.LoggerListener");
        DEFAULT_GROUP_LOGGERS = Collections.unmodifiableMap(loggers);
    }

    private static final Map<LogLevel, List<String>> BOOT_LOGGING_LOGGERS;

    static {
        MultiValueMap<LogLevel, String> loggers = new LinkedMultiValueMap<>();
        loggers.add(LogLevel.DEBUG, "sql");
        loggers.add(LogLevel.DEBUG, "web");
        loggers.add(LogLevel.DEBUG, "org.springframework.boot");
        loggers.add(LogLevel.TRACE, "org.springframework");
        loggers.add(LogLevel.TRACE, "org.apache.tomcat");
        loggers.add(LogLevel.TRACE, "org.apache.catalina");
        loggers.add(LogLevel.TRACE, "org.eclipse.jetty");
        loggers.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
        BOOT_LOGGING_LOGGERS = Collections.unmodifiableMap(loggers);
    }

    protected volatile boolean initialized = false;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ClassLoader classLoader;
    private LoggingSystem loggingSystem;
    private LogFile logFile;
    private LoggerGroups loggerGroups;
    private boolean parseArgs = true;
    private LogLevel bootLogging = null;

    public LoggingBootstrap(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.loggingSystem = LoggingSystem.get(classLoader);
        this.loggingSystem.beforeInitialize();
    }

    public LoggingBootstrap() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * 设置给定记录器的记录级别
     */
    public void setLogLevel(String loggerName, LogLevel level) {
        BiConsumer<String, LogLevel> customizer = getLogLevelConfigurer(loggingSystem);
        customizer.accept(loggerName, level);
    }

    /**
     * 初始化日志模块
     */
    public void init(ConfigurableEnvironment environment) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        if (this.loggingSystem == null) {
            this.loggingSystem = LoggingSystem.get(classLoader);
        }
        Binder.get(environment).bind("spring.output.ansi.enabled", AnsiOutput.Enabled.class).ifBound(AnsiOutput::setEnabled);
        AnsiOutput.setConsoleAvailable(environment.getProperty("spring.output.ansi.console-available", Boolean.class));
        initialize(environment);
    }

    /**
     * 销毁日志模块
     */
    public void destroy() {
        if (this.loggingSystem != null) {
            this.loggingSystem.cleanUp();
            Runnable shutdownHandler = loggingSystem.getShutdownHandler();
            if (shutdownHandler != null) {
                shutdownHandler.run();
            }
        }
    }

    /**
     * 根据通过 {@link Environment} 和类路径表示的首选项初始化日志系统
     *
     * @param environment 环境
     */
    private void initialize(ConfigurableEnvironment environment) {
        getLoggingSystemProperties(environment).apply();
        this.logFile = LogFile.get(environment);
        if (this.logFile != null) {
            this.logFile.applyToSystemProperties();
        }
        this.loggerGroups = new LoggerGroups(DEFAULT_GROUP_LOGGERS);
        initializeEarlyLoggingLevel(environment);
        initializeSystem(environment, this.loggingSystem, this.logFile);
        initializeFinalLoggingLevels(environment, this.loggingSystem);
    }

    /**
     * 获取日志系统属性
     */
    private LoggingSystemProperties getLoggingSystemProperties(ConfigurableEnvironment environment) {
        return (this.loggingSystem != null) ? this.loggingSystem.getSystemProperties(environment) : new LoggingSystemProperties(environment);
    }

    /**
     * 初始化早期日志记录级别
     */
    private void initializeEarlyLoggingLevel(ConfigurableEnvironment environment) {
        if (this.parseArgs && this.bootLogging == null) {
            if (isSet(environment, "debug")) {
                this.bootLogging = LogLevel.DEBUG;
            }
            if (isSet(environment, "trace")) {
                this.bootLogging = LogLevel.TRACE;
            }
        }
    }

    private boolean isSet(ConfigurableEnvironment environment, String property) {
        String value = environment.getProperty(property);
        return (value != null && !value.equals("false"));
    }

    private void initializeSystem(ConfigurableEnvironment environment, LoggingSystem system, LogFile logFile) {
        String logConfig = environment.getProperty(CONFIG_PROPERTY);
        if (StringUtils.hasLength(logConfig)) {
            logConfig = logConfig.strip();
        }
        try {
            LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
            if (ignoreLogConfig(logConfig)) {
                system.initialize(initializationContext, null, logFile);
            } else {
                system.initialize(initializationContext, logConfig, logFile);
            }
        } catch (Exception ex) {
            Throwable exceptionToReport = ex;
            while (exceptionToReport != null && !(exceptionToReport instanceof FileNotFoundException)) {
                exceptionToReport = exceptionToReport.getCause();
            }
            exceptionToReport = (exceptionToReport != null) ? exceptionToReport : ex;
            // NOTE: We can't use the logger here to report the problem
            System.err.println("Logging system failed to initialize using configuration from '" + logConfig + "'");
            exceptionToReport.printStackTrace(System.err);
            throw new IllegalStateException(ex);
        }
    }

    private boolean ignoreLogConfig(String logConfig) {
        return !StringUtils.hasLength(logConfig) || logConfig.startsWith("-D");
    }

    private void initializeFinalLoggingLevels(ConfigurableEnvironment environment, LoggingSystem system) {
        bindLoggerGroups(environment);
        if (this.bootLogging != null) {
            initializeBootLogging(system, this.bootLogging);
        }
        setLogLevels(system, environment);
    }

    private void bindLoggerGroups(ConfigurableEnvironment environment) {
        if (this.loggerGroups != null) {
            Binder binder = Binder.get(environment);
            binder.bind(LOGGING_GROUP, STRING_STRINGS_MAP).ifBound(this.loggerGroups::putAll);
        }
    }

    /**
     * 基于 {@link #setBootLogging(LogLevel)} 设置初始化记录器。
     * 默认情况下，此实现将根据级别选择一组适当的记录器进行配置。
     *
     * @param system      LoggingSystem
     * @param bootLogging LogLevel
     */
    private void initializeBootLogging(LoggingSystem system, LogLevel bootLogging) {
        BiConsumer<String, LogLevel> configurer = getLogLevelConfigurer(system);
        BOOT_LOGGING_LOGGERS.getOrDefault(bootLogging, Collections.emptyList()).forEach((name) -> configureLogLevel(name, bootLogging, configurer));
    }

    /**
     * 根据相关{@link Environment}属性设置日志记录级别
     *
     * @param system      LoggingSystem
     * @param environment environment
     */
    private void setLogLevels(LoggingSystem system, ConfigurableEnvironment environment) {
        BiConsumer<String, LogLevel> customizer = getLogLevelConfigurer(system);
        Binder binder = Binder.get(environment);
        Map<String, LogLevel> levels = binder.bind(LOGGING_LEVEL, STRING_LOGLEVEL_MAP).orElseGet(Collections::emptyMap);
        levels.forEach((name, level) -> configureLogLevel(name, level, customizer));
    }

    private void configureLogLevel(String name, LogLevel level, BiConsumer<String, LogLevel> configurer) {
        if (this.loggerGroups != null) {
            LoggerGroup group = this.loggerGroups.get(name);
            if (group != null && group.hasMembers()) {
                group.configureLogLevel(level, configurer);
                return;
            }
        }
        configurer.accept(name, level);
    }

    private BiConsumer<String, LogLevel> getLogLevelConfigurer(LoggingSystem system) {
        return (name, level) -> {
            try {
                name = name.equalsIgnoreCase(LoggingSystem.ROOT_LOGGER_NAME) ? null : name;
                system.setLogLevel(name, level);
            } catch (RuntimeException ex) {
                this.logger.error(String.format("Cannot set level '%s' for '%s'", level, name));
            }
        };
    }

    /**
     * 设置用于引导和相关库的自定义日志记录级别。
     *
     * @param bootLogging 日志记录级别
     */
    public void setBootLogging(LogLevel bootLogging) {
        this.bootLogging = bootLogging;
    }

    /**
     * 设置是否应为 {@literal debug} 和 {@literal trace} 属性(通常从 {@literal --debug} 或 {@literal --trace} 命令行参数定义)分析初始化参数。
     * 默认为 {@code true}。
     *
     * @param parseArgs 如果应该分析参数
     */
    public void setParseArgs(boolean parseArgs) {
        this.parseArgs = parseArgs;
    }

    public LogFile getLogFile() {
        return logFile;
    }
}
