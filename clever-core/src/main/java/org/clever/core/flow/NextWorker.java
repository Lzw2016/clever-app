package org.clever.core.flow;

import lombok.Data;
import org.clever.util.Assert;

/**
 * 之后的任务节点
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 13:54 <br/>
 */
@Data
public class NextWorker {
    /**
     * 当前任务节点
     */
    private final WorkerNode current;
    /**
     * 之后的任务节点
     */
    private final WorkerNode next;
    /**
     * 如果 next 任务已经执行完成，能否跳过 current 任务执行
     */
    private final boolean canSkip;

    /**
     * @param current 当前任务节点
     * @param next    之后的任务节点
     * @param canSkip 如果 next 任务已经执行完成，能否跳过 current 任务执行
     */
    public NextWorker(WorkerNode current, WorkerNode next, boolean canSkip) {
        Assert.notNull(current, "参数 current 不能为 null");
        Assert.notNull(next, "参数 next 不能为 null");
        this.current = current;
        this.next = next;
        this.canSkip = canSkip;
    }
}
