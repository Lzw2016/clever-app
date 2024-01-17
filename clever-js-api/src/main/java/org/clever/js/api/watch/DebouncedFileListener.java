package org.clever.js.api.watch;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.clever.util.Assert;

import java.io.File;
import java.io.FileFilter;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 16:00 <br/>
 */
public class DebouncedFileListener implements FileAlterationListener {
    private final FileFilter fileFilter;
    private final Debounced<FileSystemWatcher.MonitorEvent> debounced;

    public DebouncedFileListener(BlackWhiteFileFilter fileFilter, Consumer<FileSystemWatcher.MonitorEvent> listener, long delayMillis) {
        Assert.notNull(listener, "参数listener不能为空");
        this.fileFilter = fileFilter;
        this.debounced = new Debounced<>(listener, delayMillis);
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
    }

    @Override
    public void onDirectoryCreate(File directory) {
        if (fileFilter != null && !fileFilter.accept(directory)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.DirectoryCreate, directory));
    }

    @Override
    public void onDirectoryChange(File directory) {
        if (fileFilter != null && !fileFilter.accept(directory)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.DirectoryChange, directory));
    }

    @Override
    public void onDirectoryDelete(File directory) {
        if (fileFilter != null && !fileFilter.accept(directory)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.DirectoryDelete, directory));
    }

    @Override
    public void onFileCreate(File file) {
        if (fileFilter != null && !fileFilter.accept(file)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.FileCreate, file));
    }

    @Override
    public void onFileChange(File file) {
        if (fileFilter != null && !fileFilter.accept(file)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.FileChange, file));
    }

    @Override
    public void onFileDelete(File file) {
        if (fileFilter != null && !fileFilter.accept(file)) {
            return;
        }
        debounced.execute(new FileSystemWatcher.MonitorEvent(MonitorEventType.FileDelete, file));
    }
}
