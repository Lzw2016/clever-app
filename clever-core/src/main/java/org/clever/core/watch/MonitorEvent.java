package org.clever.core.watch;

import lombok.Getter;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/02/06 14:30 <br/>
 */
@Getter
public class MonitorEvent {
    /**
     * 事件类型
     */
    private final MonitorEventType eventType;
    /**
     * 事件对应的文件或文件夹
     */
    private final File fileOrDir;

    public MonitorEvent(MonitorEventType eventType, File fileOrDir) {
        this.eventType = eventType;
        this.fileOrDir = fileOrDir;
    }
}
