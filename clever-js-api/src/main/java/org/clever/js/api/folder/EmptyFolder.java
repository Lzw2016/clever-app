package org.clever.js.api.folder;

import org.apache.commons.io.FilenameUtils;

import java.util.List;

/**
 * 空的 Folder, 当不需要使用 require 时使用的 Folder
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/02/04 22:18 <br/>
 */
public class EmptyFolder implements Folder {
    private static final String NOT_EXISTS_PATH = "/not_exists_path";
    public static final EmptyFolder ROOT = new EmptyFolder(NOT_EXISTS_PATH);

    private final String fullPath;

    public EmptyFolder(String fullPath) {
        this.fullPath = fullPath;
    }

    @Override
    public Folder getRoot() {
        return ROOT;
    }

    @Override
    public Folder getParent() {
        return this.concat(Folder.Parent_Path);
    }

    @Override
    public String getName() {
        return FilenameUtils.getName(fullPath);
    }

    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Override
    public String getAbsolutePath() {
        return fullPath;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public String getFileContent(String name) {
        return null;
    }

    @Override
    public String getFileContent() {
        return null;
    }

    @Override
    public List<Folder> getChildren() {
        return null;
    }

    @Override
    public Folder concat(String... paths) {
        return new EmptyFolder(AbstractFolder.concatPath(fullPath, paths));
    }

    @Override
    public Folder create(String path) {
        return new EmptyFolder(AbstractFolder.concatPath(fullPath, path));
    }
}
