package org.clever.core.flow;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.clever.core.thread.SharedThreadPoolExecutor;
import org.clever.util.Assert;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 任务上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:21 <br/>
 */
@Slf4j
public class WorkerContext {
    public static final WorkerContext NULL = new WorkerContext(
        SharedThreadPoolExecutor.getCachedPool(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyList()
    );

    /**
     * 执行任务使用的线程池
     */
    @Getter
    private final ExecutorService executor;
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
     * @param executor         执行任务使用的线程池
     * @param flattenWorkers   平铺的所有任务节点
     * @param flattenWorkerMap 平铺的所有任务节点 {@code Map<WorkerNode唯一ID, WorkerNode>}
     * @param entryWorkers     入口任务集合
     */
    public WorkerContext(ExecutorService executor, List<WorkerNode> flattenWorkers, Map<String, WorkerNode> flattenWorkerMap, List<WorkerNode> entryWorkers) {
        this.executor = executor;
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
     * 等待指定的任务完成。<br/>
     * <b>注意: 滥用此函数容易产生死锁！</b><br/>
     * <pre>{@code
     * // 1.阻塞当前调用线程，等待指定的任务完成
     * CompletableFuture<List<Object>> future = context.await(worker_1, worker_2, ...);
     * future.join();
     *
     * // 2.阻塞当前调用线程，等待指定的任务完成，最多等待3秒，超时会触发 TimeoutException 异常
     * CompletableFuture<List<Object>> future = context.await(worker_1, worker_2, ...);
     * future.get(3, TimeUnit.SECONDS);
     *
     * // 3.不阻塞当前调用线程，指定的任务完成后异步通知
     * CompletableFuture<List<Object>> future = context.await(worker_1, worker_2, ...);
     * future.thenAccept((results) -> {
     *     // 任务执行完成后，异步通知
     * });
     * }</pre>
     *
     * @param awaitWorkers 指定的任务
     */
    public CompletableFuture<List<Object>> await(WorkerNode... awaitWorkers) {
        if (awaitWorkers == null || awaitWorkers.length == 0) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        for (WorkerNode worker : awaitWorkers) {
            Assert.notNull(worker, "参数 awaitWorkers 中不能有 null 元素");
            Assert.isTrue(flattenWorkerMap.containsKey(worker.getId()), String.format("WorkerNode(id=%s, name=%s)不属于当前任务流程", worker.getId(), worker.getName()));
        }
        return CompletableFuture.supplyAsync(() -> {
            List<WorkerNode> futureWorkers = Arrays.stream(awaitWorkers).filter(WorkerNode::notCompleted).collect(Collectors.toList());
            while (!futureWorkers.isEmpty()) {
                Set<String> futureWorkerIds = futureWorkers.stream().map(WorkerNode::getId).collect(Collectors.toSet());
                boolean notFound = true;
                TraceWorker traceWorker = firstTrace;
                while (traceWorker != null) {
                    Boolean realRun = traceWorker.getRealRun();
                    WorkerNode current = traceWorker.getCurrent();
                    CompletableFuture<Void> future = traceWorker.getFuture();
                    if (!Objects.equals(realRun, false) && future != null && !future.isDone() && futureWorkerIds.contains(current.getId())) {
                        notFound = false;
                        try {
                            future.join();
                        } catch (Throwable ignored) {
                        }
                    }
                    traceWorker = traceWorker.getNextTrace();
                }
                if (notFound) {
                    try {
                        // noinspection BusyWait
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        log.warn("休眠被中断", e);
                    }
                }
                futureWorkers = futureWorkers.stream().filter(WorkerNode::notCompleted).collect(Collectors.toList());
            }
            return Arrays.stream(awaitWorkers).map(worker -> getResult(worker.getId())).collect(Collectors.toList());
        }, executor);
    }

    /**
     * 获取任务真实的执行日志信息
     *
     * @param onlyRealRun 是否仅仅包含 “记录了真实执行过程” 的 TraceWorker
     */
    public String traceLog(boolean onlyRealRun) {
        // ↑ | ↓ | ← | →
        final String line = "\n";
        final String down = " ↓";
        final StringBuilder logs = new StringBuilder();
        final List<Map<String, String>> infos = new ArrayList<>();
        final Map<String, Integer> maxWidths = new HashMap<>(16);
        TraceWorker traceWorker = firstTrace;
        // 收集信息
        while (traceWorker != null) {
            // realRun 过滤
            if (onlyRealRun && Objects.equals(traceWorker.getRealRun(), false)) {
                traceWorker = traceWorker.getNextTrace();
                continue;
            }
            WorkerNode from = traceWorker.getFrom();
            WorkerNode workerNode = traceWorker.getCurrent();
            int cost = traceWorker.cost();
            int await = traceWorker.await();
            // 计算属性
            String id = workerNode.getId();
            String name = workerNode.getName();
            String fromName = String.valueOf(from == null ? "null" : from.getName());
            String state = workerNode.getStateText();
            String realRun = String.valueOf(traceWorker.getRealRun());
            String start = traceWorker.getStart() <= 0 ? "null" : DateFormatUtils.format(new Date(traceWorker.getStart()), "yyyy-MM-dd HH:mm:ss.SSS");
            String awaitStr = String.valueOf(await < 0 ? "?" : await);
            String costStr = String.valueOf(cost < 0 ? "?" : cost);
            String thread = traceWorker.getThread();
            String err = traceWorker.getErr() == null ? "null" : traceWorker.getErr().getMessage();
            // 保存属性
            Map<String, String> info = new HashMap<>(16);
            info.put("id", id);
            info.put("name", name);
            info.put("from", fromName);
            info.put("state", state);
            info.put("realRun", realRun);
            info.put("start", start);
            info.put("await", awaitStr);
            info.put("cost", costStr);
            info.put("thread", thread);
            info.put("err", err);
            infos.add(info);
            // 下一个
            traceWorker = traceWorker.getNextTrace();
        }
        // 计算最大宽度
        for (Map<String, String> info : infos) {
            info.forEach((name, value) -> {
                int maxWidth = Math.max(StringUtils.length(value), maxWidths.getOrDefault(name, 0));
                maxWidths.put(name, maxWidth);
            });
        }
        // 输出内容
        for (Map<String, String> info : infos) {
            if (logs.length() > 0) {
                logs.append(down).append(line);
            }
            // [id=, name=, from=, state=, start=, await=, cost=, thread=]
            logs.append("[from=").append(StringUtils.rightPad(info.get("from"), maxWidths.get("from")))
                .append(", name=").append(StringUtils.rightPad(info.get("name"), maxWidths.get("name")))
                .append(", state=").append(StringUtils.rightPad(info.get("state"), maxWidths.get("state")));
            if (!onlyRealRun) {
                logs.append(", realRun=").append(StringUtils.rightPad(info.get("realRun"), maxWidths.get("realRun")));
            }
            logs.append(", start=").append(StringUtils.rightPad(info.get("start"), maxWidths.get("start")))
                .append(", await=").append(StringUtils.rightPad(info.get("await"), maxWidths.get("await")))
                .append(", cost=").append(StringUtils.rightPad(info.get("cost"), maxWidths.get("cost")))
                .append(", thread=").append(StringUtils.rightPad(info.get("thread"), maxWidths.get("thread")))
                .append(", id=").append(StringUtils.rightPad(info.get("id"), maxWidths.get("id")))
                .append(", err=").append(StringUtils.rightPad(info.get("err"), maxWidths.get("err")))
                .append("]").append(line);
        }
        if (!flowCompleted) {
            logs.append(line).append(down).append(line).append("...(执行中)").append(line);
        }
        return logs.toString();
    }

    /**
     * 获取任务真实的执行日志信息
     */
    public String traceLog() {
        return traceLog(true);
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
