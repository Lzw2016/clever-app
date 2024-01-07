package org.clever.core.flow;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 13:45 <br/>
 */
public interface WorkerState {
    /**
     * 初始状态
     */
    int INIT = 0;
    /**
     * 正在运行
     */
    int RUNNING = 1;
    /**
     * 执行成功
     */
    int SUCCESS = 2;
    /**
     * 发生异常
     */
    int ERROR = 3;
    /**
     * 跳过执行
     */
    int SKIPPED = 4;
    /**
     * 执行超时
     */
    int TIMEOUT = 5;
}
