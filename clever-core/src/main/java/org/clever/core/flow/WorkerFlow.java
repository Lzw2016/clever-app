package org.clever.core.flow;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.SharedThreadPoolExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 任务流工具，用于启动任务链
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 15:20 <br/>
 */
@Slf4j
public class WorkerFlow {
    /**
     * 执行异步任务的线程池
     */
    public static final ThreadPoolExecutor DEF_POOL = SharedThreadPoolExecutor.getCachedPool();

    /**
     * 开始执行任务链，使用方式如下： <br/>
     * <pre>{@code
     * // 1.阻塞当前调用线程，等待任务链执行完毕，可以从 WorkerContext 对象中获取任务链执行的结果和详情
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.join();
     *
     * // 2.阻塞当前调用线程，等待任务链执行完成，最多等待3秒，超时会触发 TimeoutException 异常
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.get(3, TimeUnit.SECONDS);
     *
     * // 3.不阻塞当前调用线程，任务链执行完成后异步通知
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * future.thenRun(workerContext -> {
     *     // 任务执行完成后，异步通知
     * });
     * }</pre>
     *
     * @param entryWorkers    入口任务集合
     * @param executorService 任务执行器(线程池)
     */
    public static CompletableFuture<WorkerContext> start(List<WorkerNode> entryWorkers, ExecutorService executorService) {
        if (entryWorkers == null || entryWorkers.isEmpty()) {
            return CompletableFuture.completedFuture(WorkerContext.NULL);
        }
        if (executorService == null) {
            executorService = DEF_POOL;
        }
        final ExecutorService executor = executorService;
        // 构造 WorkerContext
        List<WorkerNode> flattenWorkers = flattenWorkers(entryWorkers);
        Map<String, WorkerNode> flattenWorkerMap = new HashMap<>();
        flattenWorkers.forEach(workerNode -> {
            if (flattenWorkerMap.containsKey(workerNode.getId())) {
                throw new IllegalArgumentException("重复的 WorkerNode ID=" + workerNode.getId());
            }
            flattenWorkerMap.put(workerNode.getId(), workerNode);
        });
        final WorkerContext workerContext = new WorkerContext(flattenWorkers, flattenWorkerMap, entryWorkers);
        // 开始并行执行任务
        for (WorkerNode worker : entryWorkers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> worker.start(workerContext, null, executor), executor
            );
            TraceWorker traceWorker = new TraceWorker(worker, future);
            workerContext.addTrace(traceWorker);
        }
        return CompletableFuture.supplyAsync(() -> {
            // 等待所有的 WorkerNode 执行完毕
            TraceWorker traceWorker = workerContext.getFirstTrace();
            while (traceWorker != null) {
                traceWorker.getFuture().join();
                traceWorker = traceWorker.getNextTrace();
            }
            return workerContext;
        });
    }

    /**
     * 开始执行任务链，使用方式如下： <br/>
     * <pre>{@code
     * // 1.阻塞当前调用线程，等待任务链执行完毕，可以从 WorkerContext 对象中获取任务链执行的结果和详情
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.join();
     *
     * // 2.阻塞当前调用线程，等待任务链执行完成，最多等待3秒，超时会触发 TimeoutException 异常
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.get(3, TimeUnit.SECONDS);
     *
     * // 3.不阻塞当前调用线程，任务链执行完成后异步通知
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * future.thenRun(workerContext -> {
     *     // 任务执行完成后，异步通知
     * });
     * }</pre>
     *
     * @param entryWorkers 入口任务集合
     */
    public static CompletableFuture<WorkerContext> start(List<WorkerNode> entryWorkers) {
        return start(entryWorkers, DEF_POOL);
    }

    /**
     * 开始执行任务链，使用方式如下： <br/>
     * <pre>{@code
     * // 1.阻塞当前调用线程，等待任务链执行完毕，可以从 WorkerContext 对象中获取任务链执行的结果和详情
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.join();
     *
     * // 2.阻塞当前调用线程，等待任务链执行完成，最多等待3秒，超时会触发 TimeoutException 异常
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.get(3, TimeUnit.SECONDS);
     *
     * // 3.不阻塞当前调用线程，任务链执行完成后异步通知
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * future.thenRun(workerContext -> {
     *     // 任务执行完成后，异步通知
     * });
     * }</pre>
     *
     * @param executorService 任务执行器(线程池)
     * @param entryWorkers    入口任务集合
     */
    public static CompletableFuture<WorkerContext> start(ExecutorService executorService, WorkerNode... entryWorkers) {
        List<WorkerNode> workers = Collections.emptyList();
        if (entryWorkers != null) {
            workers = Arrays.asList(entryWorkers);
        }
        return start(workers, executorService);
    }

    /**
     * 开始执行任务链，使用方式如下： <br/>
     * <pre>{@code
     * // 1.阻塞当前调用线程，等待任务链执行完毕，可以从 WorkerContext 对象中获取任务链执行的结果和详情
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.join();
     *
     * // 2.阻塞当前调用线程，等待任务链执行完成，最多等待3秒，超时会触发 TimeoutException 异常
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * WorkerContext workerContext = future.get(3, TimeUnit.SECONDS);
     *
     * // 3.不阻塞当前调用线程，任务链执行完成后异步通知
     * CompletableFuture<Void> future = WorkerFlow.start(entryWorkers, executorService);
     * future.thenRun(workerContext -> {
     *     // 任务执行完成后，异步通知
     * });
     * }</pre>
     *
     * @param entryWorkers 入口任务集合
     */
    public static CompletableFuture<WorkerContext> start(WorkerNode... entryWorkers) {
        return start(DEF_POOL, entryWorkers);
    }

    /**
     * 平铺所有的任务节点，获取所有的任务集合
     *
     * @param workers 入口任务集合
     */
    private static List<WorkerNode> flattenWorkers(List<WorkerNode> workers) {
        List<WorkerNode> currentLevel = workers;
        List<WorkerNode> nextLevel;
        List<WorkerNode> flattenWorkers = new ArrayList<>();
        while (currentLevel != null && !currentLevel.isEmpty()) {
            nextLevel = new ArrayList<>();
            flattenWorkers.addAll(currentLevel);
            for (WorkerNode worker : currentLevel) {
                List<WorkerNode> nextWorkers = worker.getNextWorkers().stream().map(NextWorker::getNext).collect(Collectors.toList());
                nextLevel.addAll(nextWorkers);
            }
            currentLevel = nextLevel;
        }
        return flattenWorkers;
    }
}
