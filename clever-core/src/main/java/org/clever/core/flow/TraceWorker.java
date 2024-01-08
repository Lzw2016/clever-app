package org.clever.core.flow;

import lombok.Getter;
import org.clever.core.SystemClock;
import org.clever.util.Assert;

import java.util.concurrent.CompletableFuture;

/**
 * 任务节点执行记录
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/08 16:50 <br/>
 */
@Getter
public class TraceWorker {
//    /**
//     * 触发 current 任务的任务节点
//     */
//    private final WorkerNode from;
    /**
     * 当前任务节点
     */
    private final WorkerNode current;
    /**
     * 执行当前任务节点返回的 CompletableFuture
     */
    @Getter
    private final CompletableFuture<Void> future;
    /**
     * 下一个任务节点执行记录
     */
    private volatile TraceWorker nextTrace;
//    /**
//     * 由 current 触发执行的任务节点
//     */
//    private final List<WorkerNode> fires = new CopyOnWriteArrayList<>();

    /**
     * current 任务开始执行时间
     */
    private final long start;
    /**
     * current 任务结束执行时间
     */
    private long end;

    /**
     * @param current 当前任务节点
     * @param future  执行当前任务节点返回的 CompletableFuture
     * @param start   任务开始执行时间
     */
    public TraceWorker(WorkerNode current, CompletableFuture<Void> future, long start) {
        this.future = future;
        Assert.notNull(current, "参数 current 不能为 null");
        this.current = current;
        this.start = start;
    }

    /**
     * @param current 当前任务节点
     * @param future  执行当前任务节点返回的 CompletableFuture
     */
    public TraceWorker(WorkerNode current, CompletableFuture<Void> future) {
        this(current, future, SystemClock.now());
    }

    /**
     * 获取 current 任务执行的耗时，如果还未执行完成返回 -1
     */
    public int cost() {
        if (end == 0) {
            return -1;
        }
        return (int) (end - start);
    }

    /**
     * @param traceWorker 下一个任务节点执行记录
     */
    void setNextTrace(TraceWorker traceWorker) {
        Assert.notNull(traceWorker, "参数 traceWorker 不能为 null");
        Assert.isNull(nextTrace, "nextTrace 已经有值,不能重复设置");
        nextTrace = traceWorker;
    }

    /**
     * 记录 current 任务结束执行时间
     */
    void end() {
        end = SystemClock.now();
    }
}
