package org.clever.core.flow;

import lombok.Getter;
import org.clever.core.Assert;
import org.clever.core.SystemClock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务节点执行记录
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/08 16:50 <br/>
 */
public class TraceWorker {
    /**
     * TraceID
     */
    @Getter
    private final String id;
    /**
     * 触发 current 任务的任务节点(当 current 是入口节点时，这个字段为 null)
     */
    @Getter
    private final WorkerNode from;
    /**
     * 当前任务节点
     */
    @Getter
    private final WorkerNode current;
    /**
     * 执行当前任务节点返回的 CompletableFuture
     */
    @Getter
    private volatile CompletableFuture<Void> future;
    /**
     * 下一个任务节点执行记录
     */
    @Getter
    private volatile TraceWorker nextTrace;
    /**
     * 由 current 触发执行的任务节点
     */
    private final List<WorkerNode> fires = new CopyOnWriteArrayList<>();
    /**
     * 执行当前任务的线程名
     */
    @Getter
    private volatile String thread;
    /**
     * 任务触发时间
     */
    @Getter
    private final long fireTime;
    /**
     * current 任务开始执行时间
     */
    @Getter
    private volatile long start = 0;
    /**
     * current 任务结束执行时间
     */
    @Getter
    private volatile long end = 0;
    /**
     * 是否记录了真实执行过程, null: 未知
     */
    @Getter
    private volatile Boolean realRun;
    /**
     * 当前任务执行错误信息
     */
    @Getter
    private volatile Throwable err;

    /**
     * @param id       TraceID
     * @param from     触发当前任务的任务节点
     * @param current  当前任务节点
     * @param fireTime 任务触发时间
     */
    public TraceWorker(String id, WorkerNode from, WorkerNode current, long fireTime) {
        Assert.isNotBlank(id, "参数 id 不能为 null");
        Assert.notNull(current, "参数 current 不能为 null");
        this.id = id;
        this.from = from;
        this.current = current;
        this.fireTime = fireTime;
    }

    /**
     * @param from    触发当前任务的任务节点
     * @param current 当前任务节点
     */
    public TraceWorker(WorkerNode from, WorkerNode current) {
        this(UUID.randomUUID().toString(), from, current, SystemClock.now());
    }

    /**
     * 由 current 触发执行的任务节点(只读集合)
     */
    public List<WorkerNode> getFires() {
        return Collections.unmodifiableList(fires);
    }

    /**
     * 任务触发到任务开始执行等待的时间，如果还未开始执行返回 -1
     */
    public int await() {
        if (start == 0) {
            return -1;
        }
        return (int) (start - fireTime);
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
     * 执行当前任务节点返回的 CompletableFuture
     */
    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
    }

    /**
     * 执行当前任务的线程名
     */
    void setThread(String thread) {
        this.thread = thread;
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
     * 新增一个由 current 触发执行的任务节点
     *
     * @param workerNode 由 current 触发执行的任务节点
     */
    void addFire(WorkerNode workerNode) {
        Assert.notNull(workerNode, "参数 workerNode 不能为 null");
        fires.add(workerNode);
    }

    /**
     * 记录 current 任务开始执行时间
     */
    void start() {
        start = SystemClock.now();
        thread = Thread.currentThread().getName();
    }

    /**
     * 记录 current 任务结束执行时间
     */
    void end() {
        end = SystemClock.now();
    }

    /**
     * 当前任务执行错误信息
     */
    void err(Throwable err) {
        this.err = err;
    }

    /**
     * 是否记录了真实执行过程
     */
    void realRun(boolean realRun) {
        this.realRun = realRun;
    }
}
