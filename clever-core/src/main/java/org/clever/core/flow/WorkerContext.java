package org.clever.core.flow;

import lombok.Getter;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 任务上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:21 <br/>
 */
public class WorkerContext {
    public static final WorkerContext NULL = new WorkerContext(Collections.emptyList(), Collections.emptyMap(), Collections.emptyList());

    /**
     * 平铺的所有任务节点
     */
    private final List<WorkerNode> flattenWorkers;
    /**
     * 平铺的所有任务节点 {@code Map<WorkerNode唯一ID, WorkerNode>}
     */
    private final Map<String, WorkerNode> flattenWorkerMap;
    /**
     * 入口任务集合
     */
    private final List<WorkerNode> entryWorkers;
    /**
     * 任务节点执行的返回值 {@code Map<WorkerNode唯一ID, Worker result>}
     */
    private final ConcurrentMap<String, Object> results = new ConcurrentHashMap<>();
    /**
     * 自定义属性
     */
    @Getter
    private final WorkerVariable attributes = new WorkerVariable();
    /**
     * 整个任务流程都执行完毕
     */
    @Getter
    private volatile boolean flowCompleted;
    /**
     * 第一个 TraceWorker(入口)
     */
    @Getter
    private volatile TraceWorker firstTrace;
    /**
     * 当前最新的 TraceWorker(最后一个)
     */
    @Getter
    private volatile TraceWorker currentTrace;
    /**
     * 所有的 TraceWorker {@code Map<WorkerNode唯一ID, TraceWorker>}
     */
    private final ConcurrentMap<String, TraceWorker> flattenTraces = new ConcurrentHashMap<>();

    /**
     * @param flattenWorkers   平铺的所有任务节点
     * @param flattenWorkerMap 平铺的所有任务节点 {@code Map<WorkerNode唯一ID, WorkerNode>}
     * @param entryWorkers     入口任务集合
     */
    public WorkerContext(List<WorkerNode> flattenWorkers, Map<String, WorkerNode> flattenWorkerMap, List<WorkerNode> entryWorkers) {
        this.flattenWorkers = flattenWorkers;
        this.flattenWorkerMap = flattenWorkerMap;
        this.entryWorkers = entryWorkers;
    }

    /**
     * 平铺的所有任务节点
     */
    public List<WorkerNode> getFlattenWorkers() {
        return Collections.unmodifiableList(flattenWorkers);
    }

    /**
     * 平铺的所有任务节点 {@code Map<WorkerNode唯一ID, WorkerNode>}
     */
    public Map<String, WorkerNode> getFlattenWorkerMap() {
        return Collections.unmodifiableMap(flattenWorkerMap);
    }

    /**
     * 入口任务集合
     */
    public List<WorkerNode> getEntryWorkers() {
        return Collections.unmodifiableList(entryWorkers);
    }

    /**
     * 根据 WorkerNode ID 获取任务节点
     *
     * @param id WorkerNode ID
     * @return 不存在返回 null
     */
    public WorkerNode getWorker(String id) {
        return flattenWorkerMap.get(id);
    }

    /**
     * 任务节点执行的返回值 {@code Map<WorkerNode唯一ID, Worker result>}
     */
    public Map<String, Object> getResults() {
        return Collections.unmodifiableMap(results);
    }

    /**
     * 获取 Worker 的返回值(如果已经执行完成)
     *
     * @param id  WorkerNode ID
     * @param def 不存在时返回的默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getResult(String id, T def) {
        return (T) results.getOrDefault(id, def);
    }

    /**
     * 获取 Worker 的返回值(如果已经执行完成)
     *
     * @param id WorkerNode ID
     */
    public <T> T getResult(String id) {
        return getResult(id, null);
    }

    /**
     * 获取所有前置任务的返回值(如果已经执行完成)
     */
    public List<Object> getPrevWorkerResults(WorkerNode current) {
        return current.getPrevWorkers().stream().map(prev -> getResult(prev.getPrev().getId())).collect(Collectors.toList());
    }

    /**
     * 获取所有后续任务的返回值(如果已经执行完成)
     */
    public List<Object> getNextWorkerResults(WorkerNode current) {
        return current.getNextWorkers().stream().map(next -> getResult(next.getNext().getId())).collect(Collectors.toList());
    }

    /**
     * 是否所有的前置任务执行完成
     */
    public boolean allPrevCompleted(WorkerNode current) {
        return current.getPrevWorkers().stream().allMatch(prev -> prev.getPrev().isCompleted());
    }

    /**
     * 是否所有的后续任务执行完成
     */
    public boolean allNextCompleted(WorkerNode current) {
        return current.getNextWorkers().stream().allMatch(next -> next.getNext().isCompleted());
    }

    /**
     * 是否所有的前置任务执行成功
     */
    public boolean allPrevSuccess(WorkerNode current) {
        return current.getPrevWorkers().stream().allMatch(prev -> prev.getPrev().isSuccess());
    }

    /**
     * 是否所有的后续任务执行成功
     */
    public boolean allNextSuccess(WorkerNode current) {
        return current.getNextWorkers().stream().allMatch(next -> next.getNext().isSuccess());
    }

    /**
     * 获取 WorkerNode 对应的 TraceWorker
     *
     * @param id WorkerNode ID
     */
    public TraceWorker getTraceWorker(String id) {
        return flattenTraces.get(id);
    }

    public String traceLog() {
        // [id=, name=, cost=] | ↑ | ↓ | ← | →
        final String line = "\n";
        final String down = " ↓";
        final StringBuilder logs = new StringBuilder();
        TraceWorker traceWorker = firstTrace;
        while (traceWorker != null) {
            if (logs.length() > 0) {
                logs.append(down).append(line);
            }
            WorkerNode workerNode = traceWorker.getCurrent();
            int cost = traceWorker.cost();
            logs.append("[id=").append(workerNode.getId())
                .append(", name=").append(workerNode.getName())
                .append(", start=").append(DateFormatUtils.format(new Date(traceWorker.getStart()), "yyyy-MM-dd HH:mm:ss.SSS"))
                .append(", cost=").append(cost <= 0 ? "?" : cost)
                .append(", thread=").append(traceWorker.getThread())
                .append("]").append(line);
            traceWorker = traceWorker.getNextTrace();
        }
        if (!flowCompleted) {
            logs.append(line).append(down).append(line).append("...(执行中)").append(line);
        }
        return logs.toString();
    }

    /**
     * 设置任务节点执行的返回值
     *
     * @param worker 任务节点
     * @param result 执行的返回值
     */
    void setResult(WorkerNode worker, Object result) {
        if (result == null) {
            return;
        }
        results.put(worker.getId(), result);
    }

    /**
     * 增加任务节点执行记录
     *
     * @param nextTrace 下一个任务节点执行记录
     */
    synchronized void addTrace(TraceWorker nextTrace) {
        Assert.notNull(nextTrace, "参数 nextTrace 不能为 null");
        flattenTraces.put(nextTrace.getCurrent().getId(), nextTrace);
        if (firstTrace == null) {
            firstTrace = nextTrace;
        }
        if (currentTrace != null) {
            currentTrace.setNextTrace(nextTrace);
        }
        currentTrace = nextTrace;
    }

    /**
     * 设置整个任务流程都执行完毕
     */
    void flowCompleted() {
        flowCompleted = true;
    }
}
