package org.clever.task.core.support;

/**
 * 定时任务调度器实例状态
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/02 17:03 <br/>
 */
public enum TaskState {
    /**
     * 未初始化
     */
    None,
    /**
     * 初始化中
     */
    Initializing,
    /**
     * 运行中
     */
    Running,
    /**
     * 暂停
     */
    Pause,
    /**
     * 已停止
     */
    Stopped,
}
