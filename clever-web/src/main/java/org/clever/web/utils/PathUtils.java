package org.clever.web.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.clever.core.io.DefaultResourceLoader;
import org.clever.core.io.Resource;
import org.clever.util.ResourceUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/23 22:02 <br/>
 */
@Slf4j
public class PathUtils {
    private static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    /**
     * 返回相对于 {@code basePath} 的资源
     *
     * @param basePath     基础路径
     * @param relativePath 相对路径
     */
    public static Resource getResource(String basePath, String relativePath) {
        if (basePath == null) {
            basePath = "./";
        }
        if (relativePath == null) {
            relativePath = "./";
        }
        String path;
        // 处理相对路径
        if (relativePath.startsWith("/")
                || relativePath.startsWith(ResourceUtils.FILE_URL_PREFIX)
                || relativePath.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            path = relativePath;
        } else {
            path = FilenameUtils.concat(basePath, relativePath);
        }
        path = FilenameUtils.normalizeNoEndSeparator(path, true);
        if (path == null) {
            path = "./";
        }
        if (!path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)
                && !path.startsWith(ResourceUtils.JAR_URL_PREFIX)
                && !path.startsWith(ResourceUtils.WAR_URL_PREFIX)) {
            path = ResourceUtils.FILE_URL_PREFIX + path;
        }
        // 判断resource是文件还是目录
        Resource resource = RESOURCE_LOADER.getResource(path);
        if (path.endsWith("/") || isExistsFile(resource)) {
            return resource;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return RESOURCE_LOADER.getResource(path);
    }

    /**
     * 返回Resource的绝对路径
     */
    @SneakyThrows
    public static String getAbsolutePath(Resource resource) {
        if (resource.isFile()) {
            return FilenameUtils.normalizeNoEndSeparator(resource.getFile().getAbsolutePath(), true);
        }
        return resource.getURL().toExternalForm();
    }

    /**
     * 判断Resource是否是一个存在的文件
     */
    public static boolean isExistsFile(Resource resource) {
        if (resource.isFile()) {
            try {
                if (!resource.getFile().isFile()) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("Resource.getFile()异常", e);
            }
        }
        boolean exists = resource.exists() && resource.isReadable();
        if (!exists) {
            return false;
        }
        long contentLength = -1L;
        try {
            contentLength = resource.contentLength();
        } catch (Exception ignored) {
        }
        return contentLength > 0L;
    }
}
