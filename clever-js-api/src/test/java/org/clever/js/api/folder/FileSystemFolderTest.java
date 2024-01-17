package org.clever.js.api.folder;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 13:36 <br/>
 */
@Slf4j
public class FileSystemFolderTest {

    @Test
    public void t01() {
        String basePath = new File("").getAbsolutePath();
        log.info("### basePath      -> {}", basePath);
        FileSystemFolder folder = FileSystemFolder.createRootPath(basePath);
        log.info("### toString      -> {}", folder.toString());
        log.info("### isFile        -> {}", folder.isFile());
        log.info("### isDir         -> {}", folder.isDir());
        log.info("### exists        -> {}", folder.exists());
        log.info("### Parent        -> {}", folder.getParent());
        log.info("### Name          -> {}", folder.getName());
        log.info("### FullPath      -> {}", folder.getFullPath());
        log.info("### AbsolutePath  -> {}", folder.getAbsolutePath());
        log.info("### FileContent   -> {}", folder.getFileContent());
        log.info("### FileContent   -> {}", folder.getFileContent("pom.xml"));
        log.info("### Children      -> {}", folder.getChildren());
        Folder folder2 = folder.concat("src", "main", "java");
        log.info("### FullPath      -> {}", folder2.getFullPath());
        log.info("### Root          -> {}", folder2.getRoot());
        Folder folder3 = folder.create("pom.xml");
        log.info("### isFile        -> {}", folder3.isFile());
    }
}
