package org.clever.boot.logging;

import org.clever.boot.system.ApplicationPid;
import org.clever.core.env.ConfigurableEnvironment;
import org.clever.core.env.Environment;
import org.clever.core.env.PropertyResolver;
import org.clever.core.env.PropertySourcesPropertyResolver;
import org.clever.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;


/**
 * 用于设置以后可由日志配置文件使用的系统属性的实用程序。
 *
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:35 <br/>
 */
public class LoggingSystemProperties {
    /**
     * 包含进程 ID 的系统属性的名称。
     */
    public static final String PID_KEY = "PID";
    /**
     * 包含异常转换字的系统属性的名称。
     */
    public static final String EXCEPTION_CONVERSION_WORD = "LOG_EXCEPTION_CONVERSION_WORD";
    /**
     * 包含日志文件的系统属性的名称。
     */
    public static final String LOG_FILE = "LOG_FILE";
    /**
     * 包含日志路径的系统属性的名称。
     */
    public static final String LOG_PATH = "LOG_PATH";
    /**
     * 包含控制台日志模式的系统属性的名称。
     */
    public static final String CONSOLE_LOG_PATTERN = "CONSOLE_LOG_PATTERN";
    /**
     * 包含控制台日志字符集的系统属性的名称。
     */
    public static final String CONSOLE_LOG_CHARSET = "CONSOLE_LOG_CHARSET";
    /**
     * 包含文件日志模式的系统属性的名称。
     */
    public static final String FILE_LOG_PATTERN = "FILE_LOG_PATTERN";
    /**
     * 包含文件日志字符集的系统属性的名称。
     */
    public static final String FILE_LOG_CHARSET = "FILE_LOG_CHARSET";
    /**
     * 包含日志级别模式的系统属性的名称。
     */
    public static final String LOG_LEVEL_PATTERN = "LOG_LEVEL_PATTERN";
    /**
     * 包含日志日期格式模式的系统属性的名称。
     */
    public static final String LOG_DATEFORMAT_PATTERN = "LOG_DATEFORMAT_PATTERN";

    private static final BiConsumer<String, String> systemPropertySetter = (name, value) -> {
        if (System.getProperty(name) == null && value != null) {
            System.setProperty(name, value);
        }
    };

    private final Environment environment;

    private final BiConsumer<String, String> setter;

    /**
     * 创建一个新的 {@link LoggingSystemProperties} 实例。
     * @param environment the source environment
     */
    public LoggingSystemProperties(Environment environment) {
        this(environment, systemPropertySetter);
    }

    /**
     * 创建一个新的 {@link LoggingSystemProperties} 实例。
     * @param environment 源环境
     * @param setter 用于应用属性的 setter
     */
    public LoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
        Assert.notNull(environment, "Environment must not be null");
        Assert.notNull(setter, "Setter must not be null");
        this.environment = environment;
        this.setter = setter;
    }

    protected Charset getDefaultCharset() {
        return StandardCharsets.UTF_8;
    }

    public final void apply() {
        apply(null);
    }

    public final void apply(LogFile logFile) {
        PropertyResolver resolver = getPropertyResolver();
        apply(logFile, resolver);
    }

    protected void apply(LogFile logFile, PropertyResolver resolver) {
        setSystemProperty(resolver, EXCEPTION_CONVERSION_WORD, "logging.exception-conversion-word");
        setSystemProperty(PID_KEY, new ApplicationPid().toString());
        setSystemProperty(resolver, CONSOLE_LOG_PATTERN, "logging.pattern.console");
        setSystemProperty(resolver, CONSOLE_LOG_CHARSET, "logging.charset.console", getDefaultCharset().name());
        setSystemProperty(resolver, LOG_DATEFORMAT_PATTERN, "logging.pattern.dateformat");
        setSystemProperty(resolver, FILE_LOG_PATTERN, "logging.pattern.file");
        setSystemProperty(resolver, FILE_LOG_CHARSET, "logging.charset.file", getDefaultCharset().name());
        setSystemProperty(resolver, LOG_LEVEL_PATTERN, "logging.pattern.level");
        if (logFile != null) {
            logFile.applyToSystemProperties();
        }
    }

    private PropertyResolver getPropertyResolver() {
        if (this.environment instanceof ConfigurableEnvironment) {
            PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
                    ((ConfigurableEnvironment) this.environment).getPropertySources()
            );
            resolver.setConversionService(((ConfigurableEnvironment) this.environment).getConversionService());
            resolver.setIgnoreUnresolvableNestedPlaceholders(true);
            return resolver;
        }
        return this.environment;
    }

    protected final void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName) {
        setSystemProperty(resolver, systemPropertyName, propertyName, null);
    }

    protected final void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName, String defaultValue) {
        String value = resolver.getProperty(propertyName);
        value = (value != null) ? value : defaultValue;
        setSystemProperty(systemPropertyName, value);
    }

    protected final void setSystemProperty(String name, String value) {
        this.setter.accept(name, value);
    }
}
