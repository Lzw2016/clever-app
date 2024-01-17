package org.clever.js.api.folder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 操作系统文件系统Folder实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 13:15 <br/>
 */
public class FileSystemFolder extends AbstractFolder {
    /**
     * 读写文件使用的编码格式
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private FileSystemFolder(String basePath) {
        super(basePath);
    }

    private FileSystemFolder(String basePath, String path) {
        super(basePath, path);
    }

    @Override
    protected String getAbsolutePath(String path) {
        File file = new File(path);
        return file.getAbsolutePath();
    }

    @Override
    protected boolean exists(String absolutePath) {
        File file = new File(absolutePath);
        return file.exists();
    }

    @Override
    protected boolean isFile(String absolutePath) {
        File file = new File(absolutePath);
        return file.isFile();
    }

    @Override
    protected String getContent(String absolutePath) {
        File file = new File(absolutePath);
        String content;
        try {
            content = FileUtils.readFileToString(file, CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    @Override
    protected List<String> getChildren(String absolutePath) {
        File file = new File(absolutePath);
        if (file.exists()) {
            String[] children = file.list();
            if (children == null) {
                return null;
            }
            return Arrays.asList(children);
        }
        return null;
    }

    @Override
    public Folder create(String path) {
        path = FilenameUtils.concat(this.fullPath, path);
        if (path == null) {
            return null;
        }
        path = replaceSeparate(path);
        return new FileSystemFolder(this.baseAbsolutePath, path);
    }

    public static FileSystemFolder createRootPath(String basePath) {
        return new FileSystemFolder(basePath);
    }
}
