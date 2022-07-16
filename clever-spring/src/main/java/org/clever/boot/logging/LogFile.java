package org.clever.boot.logging;

import org.clever.core.env.Environment;
import org.clever.core.env.PropertyResolver;
import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.io.File;
import java.util.Properties;

/**
 * 对日志输出文件的引用。使用 {@code logging.file.name} 或 {@code logging.file.path} {@link Environment} 属性指定日志输出文件。
 * 如果未指定 {@code logging.file.name} 属性，则 {@code "clever.log"} 将被写入 {@code logging.file.path} 目录。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 21:37 <br/>
 *
 * @see #get(PropertyResolver)
 */
public class LogFile {
    /**
     * 包含日志文件名的Spring属性的名称。名称可以是精确的位置，也可以是相对于当前目录的。
     */
    public static final String FILE_NAME_PROPERTY = "logging.file.name";
    /**
     * 包含写入日志文件的目录的属性的名称。
     */
    public static final String FILE_PATH_PROPERTY = "logging.file.path";

    private final String file;
    private final String path;

    /**
     * 创建一个新的 {@link LogFile} 实例。
     *
     * @param file 对要写入的文件的引用
     */
    LogFile(String file) {
        this(file, null);
    }

    /**
     * 创建一个新的 {@link LogFile} 实例。
     *
     * @param file 对要写入的文件的引用
     * @param path 如果未指定{@code file}，则使用对日志路径的引用
     */
    LogFile(String file, String path) {
        Assert.isTrue(
                StringUtils.hasLength(file) || StringUtils.hasLength(path),
                "File or Path must not be empty"
        );
        this.file = file;
        this.path = path;
    }

    /**
     * 将日志文件详细信息应用于{@code LOG_PATH}和{@code LOG_FILE}系统属性。
     */
    public void applyToSystemProperties() {
        applyTo(System.getProperties());
    }

    /**
     * 将日志文件详细信息应用于{@code LOG_PATH}和{@code LOG_FILE}映射项。
     *
     * @param properties 要应用的属性
     */
    public void applyTo(Properties properties) {
        put(properties, LoggingSystemProperties.LOG_PATH, this.path);
        put(properties, LoggingSystemProperties.LOG_FILE, toString());
    }

    private void put(Properties properties, String key, String value) {
        if (StringUtils.hasLength(value)) {
            properties.put(key, value);
        }
    }

    @Override
    public String toString() {
        if (StringUtils.hasLength(this.file)) {
            return this.file;
        }
        return new File(this.path, "clever.log").getPath();
    }

    /**
     * 从给定的 {@link Environment} 获取一个 {@link LogFile}
     *
     * @param propertyResolver 用于获取日志属性的{@link PropertyResolver}
     * @return 如果环境不包含任何合适的属性，则为{@link LogFile}或null
     */
    public static LogFile get(PropertyResolver propertyResolver) {
        String file = propertyResolver.getProperty(FILE_NAME_PROPERTY);
        String path = propertyResolver.getProperty(FILE_PATH_PROPERTY);
        if (StringUtils.hasLength(file) || StringUtils.hasLength(path)) {
            return new LogFile(file, path);
        }
        return null;
    }
}
