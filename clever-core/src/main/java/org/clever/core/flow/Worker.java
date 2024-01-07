package org.clever.core.flow;

/**
 * 任务定义
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:14 <br/>
 */
@FunctionalInterface
public interface Worker {
    /**
     * 任务需要执行的逻辑
     *
     * @param param   任务参数
     * @param worker  当前执行的任务节点
     * @param from    发起执行当前任务的任务节点(执行入口任务时为null)
     * @param context 任务上下文
     * @return 任务的返回值
     */
    Object execute(WorkerParam param, WorkerNode worker, WorkerNode from, WorkerContext context);
}
