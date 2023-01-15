package org.clever.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import org.clever.boot.logging.*;
import org.clever.core.Ordered;
import org.clever.core.annotation.Order;
import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.Environment;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogManager;


/**
 * 用于 <a href="https://logback.qos.ch">logback</a> 的 {@link LoggingSystem}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:57 <br/>
 */
public class LogbackLoggingSystem extends Slf4JLoggingSystem {
    /**
     * 静态final字段，便于Graal删除代码
     */
    public static boolean XML_ENABLED = true;
    private static final String CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";
    private static final LogLevels<Level> LEVELS = new LogLevels<>();

    static {
        LEVELS.map(LogLevel.TRACE, Level.TRACE);
        LEVELS.map(LogLevel.TRACE, Level.ALL);
        LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
        LEVELS.map(LogLevel.INFO, Level.INFO);
        LEVELS.map(LogLevel.WARN, Level.WARN);
        LEVELS.map(LogLevel.ERROR, Level.ERROR);
        LEVELS.map(LogLevel.FATAL, Level.ERROR);
        LEVELS.map(LogLevel.OFF, Level.OFF);
    }

    private static final TurboFilter FILTER = new TurboFilter() {
        @Override
        public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format, Object[] params, Throwable t) {
            return FilterReply.DENY;
        }
    };

    public LogbackLoggingSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
        return new LogbackLoggingSystemProperties(environment);
    }

    @Override
    protected String[] getStandardConfigLocations() {
        return new String[]{"logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml"};
    }

    @Override
    public void beforeInitialize() {
        LoggerContext loggerContext = getLoggerContext();
        if (isAlreadyInitialized(loggerContext)) {
            return;
        }
        super.beforeInitialize();
        loggerContext.getTurboFilterList().add(FILTER);
    }

    @Override
    public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
        LoggerContext loggerContext = getLoggerContext();
        if (isAlreadyInitialized(loggerContext)) {
            return;
        }
        super.initialize(initializationContext, configLocation, logFile);
        loggerContext.getTurboFilterList().remove(FILTER);
        markAsInitialized(loggerContext);
        if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
            getLogger(LogbackLoggingSystem.class.getName()).warn(
                    "Ignoring '" + CONFIGURATION_FILE_PROPERTY + "' system property. Please use 'logging.config' instead."
            );
        }
    }

    @Override
    protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
        LoggerContext context = getLoggerContext();
        stopAndReset(context);
        boolean debug = Boolean.getBoolean("logback.debug");
        if (debug) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
        }
        Environment environment = initializationContext.getEnvironment();
        // Apply system properties directly in case the same JVM runs multiple apps
        new LogbackLoggingSystemProperties(environment, context::putProperty).apply(logFile);
        LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context) : new LogbackConfigurator(context);
        new DefaultLogbackConfiguration(logFile).apply(configurator);
        context.setPackagingDataEnabled(true);
    }

    @Override
    protected void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
        super.loadConfiguration(initializationContext, location, logFile);
        LoggerContext loggerContext = getLoggerContext();
        stopAndReset(loggerContext);
        try {
            configureByResourceUrl(initializationContext, loggerContext, ResourceUtils.getURL(location));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
        }
        List<Status> statuses = loggerContext.getStatusManager().getCopyOfStatusList();
        StringBuilder errors = new StringBuilder();
        for (Status status : statuses) {
            if (status.getLevel() == Status.ERROR) {
                errors.append((errors.length() > 0) ? String.format("%n") : "");
                errors.append(status.toString());
            }
        }
        if (errors.length() > 0) {
            throw new IllegalStateException(String.format("Logback configuration error detected: %n%s", errors));
        }
    }

    private void configureByResourceUrl(LoggingInitializationContext initializationContext, LoggerContext loggerContext, URL url) throws JoranException {
        if (XML_ENABLED && url.toString().endsWith("xml")) {
            JoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
            configurator.setContext(loggerContext);
            configurator.doConfigure(url);
        } else {
            new ContextInitializer(loggerContext).configureByResource(url);
        }
    }

    private void stopAndReset(LoggerContext loggerContext) {
        loggerContext.stop();
        loggerContext.reset();
        if (isBridgeHandlerInstalled()) {
            addLevelChangePropagator(loggerContext);
        }
    }

    private boolean isBridgeHandlerInstalled() {
        if (!isBridgeHandlerAvailable()) {
            return false;
        }
        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        return handlers.length == 1 && handlers[0] instanceof SLF4JBridgeHandler;
    }

    private void addLevelChangePropagator(LoggerContext loggerContext) {
        LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
        levelChangePropagator.setResetJUL(true);
        levelChangePropagator.setContext(loggerContext);
        loggerContext.addListener(levelChangePropagator);
    }

    @Override
    public void cleanUp() {
        LoggerContext context = getLoggerContext();
        markAsUninitialized(context);
        super.cleanUp();
        context.getStatusManager().clear();
        context.getTurboFilterList().remove(FILTER);
    }

    @Override
    protected void reinitialize(LoggingInitializationContext initializationContext) {
        getLoggerContext().reset();
        getLoggerContext().getStatusManager().clear();
        loadConfiguration(initializationContext, getSelfInitializationConfig(), null);
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        List<LoggerConfiguration> result = new ArrayList<>();
        for (ch.qos.logback.classic.Logger logger : getLoggerContext().getLoggerList()) {
            result.add(getLoggerConfiguration(logger));
        }
        result.sort(CONFIGURATION_COMPARATOR);
        return result;
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        String name = getLoggerName(loggerName);
        LoggerContext loggerContext = getLoggerContext();
        return getLoggerConfiguration(loggerContext.exists(name));
    }

    private String getLoggerName(String name) {
        if (!StringUtils.hasLength(name) || Logger.ROOT_LOGGER_NAME.equals(name)) {
            return ROOT_LOGGER_NAME;
        }
        return name;
    }

    private LoggerConfiguration getLoggerConfiguration(ch.qos.logback.classic.Logger logger) {
        if (logger == null) {
            return null;
        }
        LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
        LogLevel effectiveLevel = LEVELS.convertNativeToSystem(logger.getEffectiveLevel());
        String name = getLoggerName(logger.getName());
        return new LoggerConfiguration(name, level, effectiveLevel);
    }

    @Override
    public Set<LogLevel> getSupportedLogLevels() {
        return LEVELS.getSupported();
    }

    @Override
    public void setLogLevel(String loggerName, LogLevel level) {
        ch.qos.logback.classic.Logger logger = getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(LEVELS.convertSystemToNative(level));
        }
    }

    @Override
    public Runnable getShutdownHandler() {
        return () -> getLoggerContext().stop();
    }

    private ch.qos.logback.classic.Logger getLogger(String name) {
        LoggerContext factory = getLoggerContext();
        return factory.getLogger(getLoggerName(name));
    }

    private LoggerContext getLoggerContext() {
        ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        Assert.isInstanceOf(
                LoggerContext.class, factory,
                () -> String.format(
                        "LoggerFactory is not a Logback LoggerContext but Logback is on "
                                + "the classpath. Either remove Logback or the competing "
                                + "implementation (%s loaded from %s). If you are using "
                                + "WebLogic you will need to add 'org.slf4j' to "
                                + "prefer-application-packages in WEB-INF/weblogic.xml",
                        factory.getClass(),
                        getLocation(factory)
                )
        );
        return (LoggerContext) factory;
    }

    private Object getLocation(ILoggerFactory factory) {
        try {
            ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                return codeSource.getLocation();
            }
        } catch (SecurityException ex) {
            // Unable to determine location
        }
        return "unknown location";
    }

    private boolean isAlreadyInitialized(LoggerContext loggerContext) {
        return loggerContext.getObject(LoggingSystem.class.getName()) != null;
    }

    private void markAsInitialized(LoggerContext loggerContext) {
        loggerContext.putObject(LoggingSystem.class.getName(), new Object());
    }

    private void markAsUninitialized(LoggerContext loggerContext) {
        loggerContext.removeObject(LoggingSystem.class.getName());
    }

    /**
     * {@link LoggingSystemFactory}，如果可能，返回{@link LogbackLoggingSystem}
     */
    @Order(Ordered.LOWEST_PRECEDENCE)
    public static class Factory implements LoggingSystemFactory {
        private static final boolean PRESENT = ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext", Factory.class.getClassLoader());

        @Override
        public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
            if (PRESENT) {
                return new LogbackLoggingSystem(classLoader);
            }
            return null;
        }
    }
}
