package org.clever.boot.context.config;

import org.clever.core.io.FileSystemResource;
import org.clever.core.io.Resource;
import org.clever.core.io.ResourceLoader;
import org.clever.core.io.support.ResourcePatternResolver;
import org.clever.util.Assert;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 用于从位置加载资源的策略界面。支持单资源和简单的通配符目录模式。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:04 <br/>
 */
class LocationResourceLoader {
    private static final Resource[] EMPTY_RESOURCES = {};
    private static final Comparator<File> FILE_PATH_COMPARATOR = Comparator.comparing(File::getAbsolutePath);
    private static final Comparator<File> FILE_NAME_COMPARATOR = Comparator.comparing(File::getName);

    private final ResourceLoader resourceLoader;

    /**
     * 创建新的 {@link LocationResourceLoader}
     *
     * @param resourceLoader 底层资源加载器
     */
    LocationResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 如果位置包含模式，则返回。
     *
     * @param location 要检查的位置
     * @return 如果位置是图案
     */
    boolean isPattern(String location) {
        return StringUtils.hasLength(location) && location.contains("*");
    }

    /**
     * 从非模式位置获取单个资源。
     *
     * @param location 位置
     * @return 资源
     * @see #isPattern(String)
     */
    Resource getResource(String location) {
        validateNonPattern(location);
        location = StringUtils.cleanPath(location);
        if (!ResourceUtils.isUrl(location)) {
            location = ResourceUtils.FILE_URL_PREFIX + location;
        }
        return this.resourceLoader.getResource(location);
    }

    private void validateNonPattern(String location) {
        Assert.state(!isPattern(location), () -> String.format("Location '%s' must not be a pattern", location));
    }

    /**
     * 从位置模式中获取多个资源。
     *
     * @param location 位置模式
     * @param type     要返回的资源类型
     * @return 资源
     * @see #isPattern(String)
     */
    Resource[] getResources(String location, ResourceType type) {
        validatePattern(location, type);
        String directoryPath = location.substring(0, location.indexOf("*/"));
        String fileName = location.substring(location.lastIndexOf("/") + 1);
        Resource resource = getResource(directoryPath);
        if (!resource.exists()) {
            return EMPTY_RESOURCES;
        }
        File file = getFile(location, resource);
        if (!file.isDirectory()) {
            return EMPTY_RESOURCES;
        }
        File[] subDirectories = file.listFiles(this::isVisibleDirectory);
        if (subDirectories == null) {
            return EMPTY_RESOURCES;
        }
        Arrays.sort(subDirectories, FILE_PATH_COMPARATOR);
        if (type == ResourceType.DIRECTORY) {
            return Arrays.stream(subDirectories).map(FileSystemResource::new).toArray(Resource[]::new);
        }
        List<Resource> resources = new ArrayList<>();
        FilenameFilter filter = (dir, name) -> name.equals(fileName);
        for (File subDirectory : subDirectories) {
            File[] files = subDirectory.listFiles(filter);
            if (files != null) {
                Arrays.sort(files, FILE_NAME_COMPARATOR);
                Arrays.stream(files).map(FileSystemResource::new).forEach(resources::add);
            }
        }
        return resources.toArray(EMPTY_RESOURCES);
    }

    private void validatePattern(String location, ResourceType type) {
        Assert.state(isPattern(location), () -> String.format("Location '%s' must be a pattern", location));
        Assert.state(
                !location.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX),
                () -> String.format("Location '%s' cannot use classpath wildcards", location)
        );
        Assert.state(
                StringUtils.countOccurrencesOf(location, "*") == 1,
                () -> String.format("Location '%s' cannot contain multiple wildcards", location)
        );
        String directoryPath = (type != ResourceType.DIRECTORY) ? location.substring(0, location.lastIndexOf("/") + 1) : location;
        Assert.state(directoryPath.endsWith("*/"), () -> String.format("Location '%s' must end with '*/'", location));
    }

    private File getFile(String patternLocation, Resource resource) {
        try {
            return resource.getFile();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load config data resource from pattern '" + patternLocation + "'", ex);
        }
    }

    private boolean isVisibleDirectory(File file) {
        return file.isDirectory() && !file.getName().startsWith("..");
    }

    /**
     * 可以返回的资源类型。
     */
    enum ResourceType {
        /**
         * 返回文件资源。
         */
        FILE,

        /**
         * 返回目录资源。
         */
        DIRECTORY
    }
}
