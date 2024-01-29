package org.clever.task.core.model;

import lombok.Data;
import org.clever.core.thread.ThreadPoolState;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/29 10:49 <br/>
 */
@Data
public class SchedulerRuntimeInfo {
    /**
     * 调度器状态
     */
    private int state;
    /**
     * 调度器状态文本
     */
    private String stateText;
    /**
     * 调度线程池状态
     */
    private ThreadPoolState scheduledExecutorState;
    /**
     * 执行任务线程池状态
     */
    private ThreadPoolState jobExecutorState;
}
