package org.clever.boot.context.config;

import org.clever.boot.origin.Origin;
import org.clever.core.io.Resource;
import org.clever.util.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 当找不到 {@link ConfigDataResource} 时引发 {@link ConfigDataNotFoundException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:20 <br/>
 */
public class ConfigDataResourceNotFoundException extends ConfigDataNotFoundException {
    private final ConfigDataResource resource;
    private final ConfigDataLocation location;

    /**
     * 创建新的 {@link ConfigDataResourceNotFoundException}
     *
     * @param resource 找不到的资源
     */
    public ConfigDataResourceNotFoundException(ConfigDataResource resource) {
        this(resource, null);
    }

    /**
     * 创建新的 {@link ConfigDataResourceNotFoundException}
     *
     * @param resource 找不到的资源
     * @param cause    异常原因
     */
    public ConfigDataResourceNotFoundException(ConfigDataResource resource, Throwable cause) {
        this(resource, null, cause);
    }

    private ConfigDataResourceNotFoundException(ConfigDataResource resource, ConfigDataLocation location, Throwable cause) {
        super(getMessage(resource, location), cause);
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
        this.location = location;
    }

    /**
     * 返回找不到的资源
     *
     * @return 资源
     */
    public ConfigDataResource getResource() {
        return this.resource;
    }

    /**
     * 返回已解析以确定资源的原始位置
     *
     * @return 位置，如果没有可用位置，则为null
     */
    public ConfigDataLocation getLocation() {
        return this.location;
    }

    @Override
    public Origin getOrigin() {
        return Origin.from(this.location);
    }

    @Override
    public String getReferenceDescription() {
        return getReferenceDescription(this.resource, this.location);
    }

    /**
     * 创建新的 {@link ConfigDataResourceNotFoundException} 具有位置的实例
     *
     * @param location 要设置的位置
     * @return 新 {@link ConfigDataResourceNotFoundException} 实例
     */
    ConfigDataResourceNotFoundException withLocation(ConfigDataLocation location) {
        return new ConfigDataResourceNotFoundException(this.resource, location, getCause());
    }

    private static String getMessage(ConfigDataResource resource, ConfigDataLocation location) {
        return String.format("Config data %s cannot be found", getReferenceDescription(resource, location));
    }

    private static String getReferenceDescription(ConfigDataResource resource, ConfigDataLocation location) {
        String description = String.format("resource '%s'", resource);
        if (location != null) {
            description += String.format(" via location '%s'", location);
        }
        return description;
    }

    /**
     * 如果指定的 {@link Path} 不存在，则抛出 {@link ConfigDataNotFoundException}
     *
     * @param resource    配置数据资源
     * @param pathToCheck 要检查的路径
     */
    public static void throwIfDoesNotExist(ConfigDataResource resource, Path pathToCheck) {
        throwIfDoesNotExist(resource, Files.exists(pathToCheck));
    }

    /**
     * 如果指定的 {@link File} 不存在，则抛出 {@link ConfigDataNotFoundException}
     *
     * @param resource    配置数据资源
     * @param fileToCheck 要检查的文件
     */
    public static void throwIfDoesNotExist(ConfigDataResource resource, File fileToCheck) {
        throwIfDoesNotExist(resource, fileToCheck.exists());
    }

    /**
     * 如果指定的 {@link Resource} 不存在，则抛出 {@link ConfigDataNotFoundException}
     *
     * @param resource        配置数据资源
     * @param resourceToCheck 要检查的资源
     */
    public static void throwIfDoesNotExist(ConfigDataResource resource, Resource resourceToCheck) {
        throwIfDoesNotExist(resource, resourceToCheck.exists());
    }

    private static void throwIfDoesNotExist(ConfigDataResource resource, boolean exists) {
        if (!exists) {
            throw new ConfigDataResourceNotFoundException(resource);
        }
    }
}
