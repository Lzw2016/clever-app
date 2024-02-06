package org.clever.core.watch;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 11:49 <br/>
 */
public enum MonitorEventType {
//    /**
//     * 文件系统观察器开始检查事件
//     */
//    Start,
//    /**
//     * 文件系统观察程序完成了检查事件
//     */
//    Stop,
    /**
     * 目录创建事件
     */
    DirectoryCreate,
    /**
     * 目录已更改事件
     */
    DirectoryChange,
    /**
     * 目录删除事件
     */
    DirectoryDelete,
    /**
     * 文件创建事件
     */
    FileCreate,
    /**
     * 文件更改事件
     */
    FileChange,
    /**
     * 文件删除事件
     */
    FileDelete,
}
