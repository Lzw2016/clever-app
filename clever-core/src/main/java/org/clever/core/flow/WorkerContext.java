package org.clever.core.flow;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:21 <br/>
 */
@EqualsAndHashCode
public class WorkerContext {
    public static final WorkerContext NULL = new WorkerContext(Collections.emptyList(), Collections.emptyMap(), Collections.emptyList());

    /**
     * 平铺的所有任务节点
     */
    private final List<WorkerNode> flattenWorkers;
    /**
     * 平铺的所有任务节点 {@code Map<id, WorkerNode>}
     */
    private final Map<String, WorkerNode> flattenWorkerMap;
    /**
     * 入口任务集合
     */
    private final List<WorkerNode> entryWorkers;
    /**
     * 任务节点执行的返回值 {@code Map<id, result>}
     */
    private final ConcurrentMap<String, Object> results = new ConcurrentHashMap<>();
    /**
     * 自定义属性
     */
    @Getter
    private final WorkerParam attributes = new WorkerParam();


//    TODO 调用链追踪
//    private final Trace

    /**
     * @param flattenWorkers   平铺的所有任务节点
     * @param flattenWorkerMap 平铺的所有任务节点 {@code Map<id, WorkerNode>}
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
     * 平铺的所有任务节点 {@code Map<id, WorkerNode>}
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
     * 设置任务节点执行的返回值
     *
     * @param worker 任务节点
     * @param result 执行的返回值
     */
    void setResult(WorkerNode worker, Object result) {
        results.put(worker.getId(), result);
    }
}
