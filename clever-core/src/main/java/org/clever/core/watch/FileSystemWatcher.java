package org.clever.core.watch;

import lombok.SneakyThrows;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 11:12 <br/>
 */
public class FileSystemWatcher {
    /**
     * 文件监听器
     */
    private final FileAlterationMonitor monitor;
    /**
     * 文件事件观察者
     */
    private final FileAlterationObserver observer;
    /**
     * Listener
     */
    private final FileAlterationListener listener;

    /**
     * @param absolutePath    监听文件绝对路径
     * @param include         包含的文件通配符(白名单)
     * @param exclude         排除的文件通配符(黑名单)
     * @param caseSensitivity 文件大小写敏感设置
     * @param listener        文件变化时的处理函数
     * @param interval        在两次文件系统检查之间等待的时间（以毫秒为单位）
     * @param delayMillis     listener处理的防抖动延时时间
     */
    public FileSystemWatcher(String absolutePath, String[] include, String[] exclude, IOCase caseSensitivity, Consumer<MonitorEvent> listener, long interval, long delayMillis) {
        this.observer = new FileAlterationObserver(absolutePath);
        this.monitor = new FileAlterationMonitor(interval);
        this.listener = new DebouncedFileListener(new BlackWhiteFileFilter(include, exclude, caseSensitivity), listener, delayMillis);
        init();
    }

    /**
     * @param absolutePath 监听文件绝对路径
     * @param include      包含的文件通配符(白名单)
     * @param exclude      排除的文件通配符(黑名单)
     * @param listener     文件变化时的处理函数
     */
    public FileSystemWatcher(String absolutePath, String[] include, String[] exclude, Consumer<MonitorEvent> listener) {
        this(absolutePath, include, exclude, null, listener, 3000, 200);
        init();
    }

    /**
     * @param absolutePath    监听文件绝对路径
     * @param include         包含的文件通配符(白名单)
     * @param exclude         排除的文件通配符(黑名单)
     * @param caseSensitivity 文件大小写敏感设置
     * @param listener        文件变化时的处理函数
     * @param interval        在两次文件系统检查之间等待的时间（以毫秒为单位）
     * @param delayMillis     listener处理的防抖动延时时间
     */
    public FileSystemWatcher(String absolutePath, Set<String> include, Set<String> exclude, IOCase caseSensitivity, Consumer<MonitorEvent> listener, long interval, long delayMillis) {
        this.observer = new FileAlterationObserver(absolutePath);
        this.monitor = new FileAlterationMonitor(interval);
        this.listener = new DebouncedFileListener(new BlackWhiteFileFilter(include, exclude, caseSensitivity), listener, delayMillis);
        init();
    }

    /**
     * @param absolutePath 监听文件绝对路径
     * @param include      包含的文件通配符(白名单)
     * @param exclude      排除的文件通配符(黑名单)
     * @param listener     文件变化时的处理函数
     */
    public FileSystemWatcher(String absolutePath, Set<String> include, Set<String> exclude, Consumer<MonitorEvent> listener) {
        this(absolutePath, include, exclude, null, listener, 3000, 200);
        init();
    }

    /**
     * 初始化监听配置
     */
    private void init() {
        observer.addListener(listener);
        monitor.addObserver(observer);
    }

    /**
     * 开始监听
     */
    @SneakyThrows
    public void start() {
        monitor.start();
    }

    /**
     * 停止监听
     */
    @SneakyThrows
    public void stop() {
        monitor.stop();
    }
}
