package org.clever.boot.logging.logback;

import ch.qos.logback.core.util.FileSize;
import org.clever.boot.logging.LogFile;
import org.clever.boot.logging.LoggingSystemProperties;
import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.ConverterNotFoundException;
import org.clever.core.env.Environment;
import org.clever.core.env.PropertyResolver;
import org.clever.util.unit.DataSize;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

/**
 * 用于 Logback 的 {@link LoggingSystemProperties}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:02 <br/>
 */
public class LogbackLoggingSystemProperties extends LoggingSystemProperties {
    /**
     * 包含滚动日志文件名模式的系统属性的名称。
     */
    public static final String ROLLINGPOLICY_FILE_NAME_PATTERN = "LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN";
    /**
     * 包含开始时清除历史记录标志的系统属性的名称。
     */
    public static final String ROLLINGPOLICY_CLEAN_HISTORY_ON_START = "LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START";
    /**
     * 包含文件日志最大大小的系统属性的名称。
     */
    public static final String ROLLINGPOLICY_MAX_FILE_SIZE = "LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE";
    /**
     * 包含文件总大小上限的系统属性的名称。
     */
    public static final String ROLLINGPOLICY_TOTAL_SIZE_CAP = "LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP";
    /**
     * 包含文件log max history的系统属性的名称。
     */
    public static final String ROLLINGPOLICY_MAX_HISTORY = "LOGBACK_ROLLINGPOLICY_MAX_HISTORY";

    public LogbackLoggingSystemProperties(Environment environment) {
        super(environment);
    }

    /**
     * 创建一个新的{@link LogbackLoggingSystemProperties}实例。
     *
     * @param environment 源环境
     * @param setter      用于应用属性的setter
     */
    public LogbackLoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
        super(environment, setter);
    }

    @Override
    protected Charset getDefaultCharset() {
        return Charset.defaultCharset();
    }

    @Override
    protected void apply(LogFile logFile, PropertyResolver resolver) {
        super.apply(logFile, resolver);
        applyRollingPolicy(resolver, ROLLINGPOLICY_FILE_NAME_PATTERN, "logging.logback.rollingpolicy.file-name-pattern", "logging.pattern.rolling-file-name");
        applyRollingPolicy(resolver, ROLLINGPOLICY_CLEAN_HISTORY_ON_START, "logging.logback.rollingpolicy.clean-history-on-start", "logging.file.clean-history-on-start");
        applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_FILE_SIZE, "logging.logback.rollingpolicy.max-file-size", "logging.file.max-size", DataSize.class);
        applyRollingPolicy(resolver, ROLLINGPOLICY_TOTAL_SIZE_CAP, "logging.logback.rollingpolicy.total-size-cap", "logging.file.total-size-cap", DataSize.class);
        applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_HISTORY, "logging.logback.rollingpolicy.max-history", "logging.file.max-history");
    }

    private void applyRollingPolicy(PropertyResolver resolver, String systemPropertyName, String propertyName, String deprecatedPropertyName) {
        applyRollingPolicy(resolver, systemPropertyName, propertyName, deprecatedPropertyName, String.class);
    }

    private <T> void applyRollingPolicy(PropertyResolver resolver, String systemPropertyName, String propertyName, String deprecatedPropertyName, Class<T> type) {
        T value = getProperty(resolver, propertyName, type);
        if (value == null) {
            value = getProperty(resolver, deprecatedPropertyName, type);
        }
        if (value != null) {
            String stringValue = String.valueOf((value instanceof DataSize) ? ((DataSize) value).toBytes() : value);
            setSystemProperty(systemPropertyName, stringValue);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(PropertyResolver resolver, String key, Class<T> type) {
        try {
            return resolver.getProperty(key, type);
        } catch (ConversionFailedException | ConverterNotFoundException ex) {
            if (type != DataSize.class) {
                throw ex;
            }
            String value = resolver.getProperty(key);
            return (T) DataSize.ofBytes(FileSize.valueOf(value).getSize());
        }
    }
}

