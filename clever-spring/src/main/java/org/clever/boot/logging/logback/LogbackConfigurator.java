package org.clever.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import org.clever.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 允许对logback进行编程配置，这通常比解析XML更快。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:02 <br/>
 */
class LogbackConfigurator {
    private final LoggerContext context;

    LogbackConfigurator(LoggerContext context) {
        Assert.notNull(context, "Context must not be null");
        this.context = context;
    }

    LoggerContext getContext() {
        return this.context;
    }

    Object getConfigurationLock() {
        return this.context.getConfigurationLock();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
        Assert.hasLength(conversionWord, "Conversion word must not be empty");
        Assert.notNull(converterClass, "Converter class must not be null");
        Map<String, String> registry = (Map<String, String>) this.context
                .getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (registry == null) {
            registry = new HashMap<>();
            this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
        }
        registry.put(conversionWord, converterClass.getName());
    }

    void appender(String name, Appender<?> appender) {
        appender.setName(name);
        start(appender);
    }

    void logger(String name, Level level) {
        logger(name, level, true);
    }

    void logger(String name, Level level, boolean additive) {
        logger(name, level, additive, null);
    }

    void logger(String name, Level level, boolean additive, Appender<ILoggingEvent> appender) {
        Logger logger = this.context.getLogger(name);
        if (level != null) {
            logger.setLevel(level);
        }
        logger.setAdditive(additive);
        if (appender != null) {
            logger.addAppender(appender);
        }
    }

    @SafeVarargs
    final void root(Level level, Appender<ILoggingEvent>... appenders) {
        Logger logger = this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (level != null) {
            logger.setLevel(level);
        }
        for (Appender<ILoggingEvent> appender : appenders) {
            logger.addAppender(appender);
        }
    }

    void start(LifeCycle lifeCycle) {
        if (lifeCycle instanceof ContextAware) {
            ((ContextAware) lifeCycle).setContext(this.context);
        }
        lifeCycle.start();
    }
}

