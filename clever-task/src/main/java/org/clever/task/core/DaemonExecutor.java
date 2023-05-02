package org.clever.task.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.util.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 守护线程执行器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 21:57 <br/>
 */
@Slf4j
public class DaemonExecutor {
    /**
     * 守护线程名称
     */
    private final String name;
    /**
     * 调度器实例名
     */
    private final String instanceName;
    /**
     * 执行器线程池
     */
    private final ScheduledExecutorService executor;
    /**
     * 线程池Future
     */
    private ScheduledFuture<?> future;
    /**
     * 任务执行锁
     */
    private final Object lock = new Object();
    /**
     * 当前是否是运行状态
     */
    @Getter
    private boolean running = false;

    public DaemonExecutor(String name, String instanceName) {
        this.name = name;
        this.instanceName = instanceName;
        executor = Executors.newSingleThreadScheduledExecutor(
                new BasicThreadFactory.Builder()
                        .namingPattern(GlobalConstant.THREAD_POOL_NAME.getOrDefault(name, "daemon_executor-pool-%d"))
                        .daemon(true)
                        .build()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                try {
                    future.cancel(true);
                    log.info("[DaemonExecutor] 线程停止成功 | {} | instanceName={}", this.name, this.instanceName);
                } catch (Exception e) {
                    log.error("[DaemonExecutor] 线程停止失败 | {} | instanceName={}", this.name, this.instanceName, e);
                }
            }
            try {
                executor.shutdownNow();
                log.info("[DaemonExecutor] 线程池停止成功 | {} | instanceName={}", this.name, this.instanceName);
            } catch (Exception e) {
                log.error("[DaemonExecutor] 线程池停止失败 | {} | instanceName={}", this.name, this.instanceName, e);
            }
        }));
    }

    /**
     * 以固定速率定期执行任务
     *
     * @param command 任务逻辑
     * @param period  两次执行任务的时间间隔(单位：毫秒)
     */
    public void scheduleAtFixedRate(final Runnable command, final long period) {
        Assert.notNull(command, "参数command不能为空");
        Assert.isTrue(period > 0, "参数period值必须大于0");
        stop();
        future = executor.scheduleAtFixedRate(() -> run(command), GlobalConstant.THREAD_POOL_INITIAL_DELAY, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止守护线程调度执行
     */
    public void stop() {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    private void run(final Runnable command) {
        if (running) {
            log.warn("[DaemonExecutor] 守护线程正在运行，等待... | {} | instanceName={}", this.name, this.instanceName);
            return;
        }
        synchronized (lock) {
            if (running) {
                log.warn("[DaemonExecutor] 守护线程正在运行，等待... | {} | instanceName={}", this.name, this.instanceName);
                return;
            }
            running = true;
            try {
                final long startTime = System.currentTimeMillis();
                command.run();
                final long endTime = System.currentTimeMillis();
                log.debug("[DaemonExecutor] 守护线程完成，耗时：{}ms | {} | instanceName={}", (endTime - startTime), this.name, this.instanceName);
            } catch (Exception e) {
                log.error("[DaemonExecutor] 守护线程异常 | {} | instanceName={}", this.name, this.instanceName, e);
            } finally {
                running = false;
            }
        }
    }
}
