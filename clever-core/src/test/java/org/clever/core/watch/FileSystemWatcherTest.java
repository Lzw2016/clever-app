package org.clever.core.watch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.springframework.util.AntPathMatcher;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 12:00 <br/>
 */
@Slf4j
public class FileSystemWatcherTest {

    @Test
    public void t01() throws InterruptedException {
        String absPath = new File("./").getAbsolutePath();
        log.info("absPath -> {}", absPath);
        FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(
            absPath,
            new String[]{"*.java"},
            new String[]{},
            IOCase.SYSTEM,
            monitorEvent -> log.info("#### --> {} | {}", monitorEvent.getEventType(), monitorEvent.getFileOrDir().getAbsolutePath()),
            1000,
            100
        );
        fileSystemWatcher.start();
        Thread.sleep(1000);
        fileSystemWatcher.stop();
        fileSystemWatcher.start();
        Thread.sleep(1000 * 30);
    }

    @Test
    public void t02() {
        // ？匹配一个字符
        // *匹配0个或多个字符
        String path = "/clever/watch/01Base.ts";
        log.info("res -> {}", FilenameUtils.wildcardMatch(path, "*.ts", IOCase.SYSTEM));
        log.info("res -> {}", FilenameUtils.wildcardMatch(path, "*\\watch\\*", IOCase.SYSTEM));
    }

    @Test
    public void t03() {
        // ？匹配一个字符
        // *匹配0个或多个字符
        // **匹配0个或多个目录
        String path = "/clever/watch/01Base.ts";
        AntPathMatcher matcher = new AntPathMatcher(File.separator);
        log.info("res -> {}", matcher.match("*\\**\\*.ts", path));
        log.info("res -> {}", matcher.match("*\\**\\clever\\**", path));
    }
}
