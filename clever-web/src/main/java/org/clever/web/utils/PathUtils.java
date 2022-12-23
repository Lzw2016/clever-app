package org.clever.web.utils;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.clever.util.ResourceUtils;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/23 22:02 <br/>
 */
public class PathUtils {
    /**
     * 返回相对于 {@code basePath} 的绝对路径
     *
     * @param basePath     基础路径
     * @param relativePath 其相对路径
     */
    @SneakyThrows
    public static String getAbsolutePath(String basePath, String relativePath) {
        final String fileProtocol = "file:";
        final String classpathProtocol = "classpath:";
        if (basePath == null) {
            basePath = "";
        }
        if (relativePath == null) {
            relativePath = "";
        }
        String path;
        // 处理相对路径
        if (relativePath.startsWith("/")
                || relativePath.startsWith(fileProtocol)
                || relativePath.startsWith(classpathProtocol)) {
            path = relativePath;
        } else {
            path = FilenameUtils.concat(basePath, relativePath);
            path = FilenameUtils.normalizeNoEndSeparator(path, true);
        }
        // 获取绝对路径
        if (path.startsWith(fileProtocol)) {
            return ResourceUtils.getFile(path).getAbsolutePath();
        }
        if (path.startsWith(classpathProtocol)) {
            return path;
        }
        return FilenameUtils.normalizeNoEndSeparator(new File(path).getAbsolutePath(), true);
    }
}
