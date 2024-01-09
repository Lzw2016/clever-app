package org.clever.core.flow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 17:29 <br/>
 */
@Slf4j
public class WorkerFlowTest {
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        0, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(),
        new BasicThreadFactory.Builder()
            .namingPattern("cached-shared-%d")
            .daemon(true)
            .build(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @SneakyThrows
    public void sleep(long time) {
        Thread.sleep(time);
    }

    public CompletableFuture<String> getFuture() {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            log.info("### 1.1");
            log.info("### 1.2");
        }).thenApplyAsync(unused -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            log.info("### 2.1");
            log.info("### 2.2");
            return "OK";
        });
    }

    @Test
    public void t01() {
        log.info("### 1");
        getFuture().join();
        log.info("### 2");
        sleep(1_000);
    }

    @Test
    public void t02() throws ExecutionException, InterruptedException {
        log.info("### 1");
        getFuture().get();
        log.info("### 2");
        sleep(1_000);
    }

    @Test
    public void t03() {
        log.info("### 1");
        getFuture().thenAccept(str -> log.info("### str={}", str));
        getFuture().thenAcceptAsync(str -> log.info("### str={}", str), threadPool);
        log.info("### 2");
        sleep(1_000);
    }

    private WorkerNode workerNode_1 = WorkerNode.Builder.create()
        .setName("01")
        .worker(context -> {
            log.info("01.1 -> PoolSize={}", threadPool.getPoolSize());
            sleep(300);
            log.info("01.2 -> PoolSize={}", threadPool.getPoolSize());
            return "worker_01";
        })
        .build();
    private WorkerNode workerNode_2 = WorkerNode.Builder.create()
        .setName("02")
        .worker(context -> {
            log.info("02.1 -> PoolSize={}", threadPool.getPoolSize());
            sleep(300);
            log.info("02.2 -> PoolSize={}", threadPool.getPoolSize());
            return "worker_02";
        })
        .build();
    private WorkerNode workerNode_3 = WorkerNode.Builder.create()
        .setName("03")
        .worker(context -> {
            log.info("03.1 -> PoolSize={}", threadPool.getPoolSize());
            sleep(300);
            log.info("03.2 -> PoolSize={}", threadPool.getPoolSize());
            return "worker_03";
        })
        .build();
    private WorkerNode workerNode_4 = WorkerNode.Builder.create()
        .setName("04")
        .worker(context -> {
            log.info("04.1 -> PoolSize={}", threadPool.getPoolSize());
            sleep(300);
            log.info("04.2 -> PoolSize={}", threadPool.getPoolSize());
            return "worker_04";
        })
        .build();
    private WorkerNode workerNode_5 = WorkerNode.Builder.create()
        .setName("05")
        .worker(context -> {
            log.info("05.1 -> PoolSize={}", threadPool.getPoolSize());
            sleep(300);
            log.info("05.2 -> PoolSize={}", threadPool.getPoolSize());
            return "worker_05";
        })
        .build();

    @Test
    public void t04() {
        workerNode_1.addNext(workerNode_2);
        workerNode_2.addNext(workerNode_3);
        workerNode_3.addNext(workerNode_4);
        workerNode_3.addNext(workerNode_5);
        workerNode_4.addNext(workerNode_5, false);
        CompletableFuture<WorkerContext> future = WorkerFlow.start(threadPool, workerNode_1);
        WorkerContext context = future.join();
        log.info("完成 \n{}", context.traceLog());

        // 重复执行
        future = WorkerFlow.start(threadPool, workerNode_1);
        context = future.join();
        log.info("完成 \n{}", context.traceLog(false));

        // clone 后重新执行
        workerNode_1 = workerNode_1.clone();
        workerNode_2 = workerNode_2.clone();
        workerNode_3 = workerNode_3.clone();
        workerNode_4 = workerNode_4.clone();
        workerNode_5 = workerNode_5.clone();
        // clone 后需要重新指定执行流程
        workerNode_1.addNext(workerNode_2);
        workerNode_2.addNext(workerNode_3);
        workerNode_3.addNext(workerNode_4);
        workerNode_3.addNext(workerNode_5);
        workerNode_4.addNext(workerNode_5, true);
        // 启动流程
        future = workerNode_1.start(threadPool);
        context = workerNode_1.getContext();
        CompletableFuture<List<Object>> awaitFuture = context.await(workerNode_1, workerNode_3);
        awaitFuture.thenAccept(res -> {
            log.info("workerNode_1 workerNode_3 完成");
            log.info("workerNode_1 workerNode_3 返回值 -> {}", res);
        });
        future.join();
        log.info("完成 \n{}", context.traceLog());
    }
}
