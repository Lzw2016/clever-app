package org.clever.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import org.clever.boot.logging.LogFile;

import java.nio.charset.Charset;

/**
 * 默认使用的logback配置。
 * 使用{@link LogbackConfigurator}来缩短启动时间。
 * 另请参阅为经典 {@code logback.xml} 使用提供的 {@code base.xml}、{@code defaults.xml}、{@code console appender.xml}和 {@code file appender.xml} 文件。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:04 <br/>
 */
class DefaultLogbackConfiguration {
    private final LogFile logFile;

    DefaultLogbackConfiguration(LogFile logFile) {
        this.logFile = logFile;
    }

    void apply(LogbackConfigurator config) {
        synchronized (config.getConfigurationLock()) {
            defaults(config);
            Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
            if (this.logFile != null) {
                Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile.toString());
                config.root(Level.INFO, consoleAppender, fileAppender);
            } else {
                config.root(Level.INFO, consoleAppender);
            }
        }
    }

    private void defaults(LogbackConfigurator config) {
        config.conversionRule("clr", ColorConverter.class);
        config.conversionRule("wex", WhitespaceThrowableProxyConverter.class);
        config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class);
        config.getContext().putProperty(
                "CONSOLE_LOG_PATTERN",
                resolve(
                        config,
                        "${CONSOLE_LOG_PATTERN:-"
                                + "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) "
                                + "%clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} "
                                + "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"
                )
        );
        String defaultCharset = Charset.defaultCharset().name();
        config.getContext().putProperty(
                "CONSOLE_LOG_CHARSET",
                resolve(config, "${CONSOLE_LOG_CHARSET:-" + defaultCharset + "}")
        );
        config.getContext().putProperty(
                "FILE_LOG_PATTERN",
                resolve(
                        config,
                        "${FILE_LOG_PATTERN:-"
                                + "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] "
                                + "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"
                )
        );
        config.getContext().putProperty(
                "FILE_LOG_CHARSET",
                resolve(config, "${FILE_LOG_CHARSET:-" + defaultCharset + "}")
        );
        config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
        config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
        config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
        config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
        config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
        config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
        config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
        config.logger("org.clever.boot.actuate.endpoint.jmx", Level.WARN);
    }

    private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern(resolve(config, "${CONSOLE_LOG_PATTERN}"));
        encoder.setCharset(resolveCharset(config, "${CONSOLE_LOG_CHARSET}"));
        config.start(encoder);
        appender.setEncoder(encoder);
        config.appender("CONSOLE", appender);
        return appender;
    }

    private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern(resolve(config, "${FILE_LOG_PATTERN}"));
        encoder.setCharset(resolveCharset(config, "${FILE_LOG_CHARSET}"));
        appender.setEncoder(encoder);
        config.start(encoder);
        appender.setFile(logFile);
        setRollingPolicy(appender, config);
        config.appender("FILE", appender);
        return appender;
    }

    private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender, LogbackConfigurator config) {
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(config.getContext());
        rollingPolicy.setFileNamePattern(resolve(config, "${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}"));
        rollingPolicy.setCleanHistoryOnStart(resolveBoolean(config, "${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}"));
        rollingPolicy.setMaxFileSize(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}"));
        rollingPolicy.setTotalSizeCap(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}"));
        rollingPolicy.setMaxHistory(resolveInt(config, "${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}"));
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.setParent(appender);
        config.start(rollingPolicy);
    }

    private boolean resolveBoolean(LogbackConfigurator config, String val) {
        return Boolean.parseBoolean(resolve(config, val));
    }

    private int resolveInt(LogbackConfigurator config, String val) {
        return Integer.parseInt(resolve(config, val));
    }

    private FileSize resolveFileSize(LogbackConfigurator config, String val) {
        return FileSize.valueOf(resolve(config, val));
    }

    private Charset resolveCharset(LogbackConfigurator config, String val) {
        return Charset.forName(resolve(config, val));
    }

    private String resolve(LogbackConfigurator config, String val) {
        return OptionHelper.substVars(val, config.getContext());
    }
}
