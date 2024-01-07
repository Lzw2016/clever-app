package org.clever.core.flow;

import lombok.Data;
import org.clever.util.Assert;

/**
 * 之前的任务节点
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 13:17 <br/>
 */
@Data
public class PrevWorker {
    /**
     * 当前任务节点
     */
    private final WorkerNode current;
    /**
     * 之前的任务节点
     */
    private final WorkerNode prev;
    /**
     * 是否必须等待 prev 任务执行完成，才能继续执行 current 任务
     */
    private final boolean waitComplete;

    /**
     * @param current      当前任务节点
     * @param prev         之前的任务节点
     * @param waitComplete 是否必须等待 prev 任务执行完成，才能继续执行 current 任务
     */
    public PrevWorker(WorkerNode current, WorkerNode prev, boolean waitComplete) {
        Assert.notNull(current, "参数 current 不能为 null");
        Assert.notNull(prev, "参数 prev 不能为 null");
        this.current = current;
        this.prev = prev;
        this.waitComplete = waitComplete;
    }
}
