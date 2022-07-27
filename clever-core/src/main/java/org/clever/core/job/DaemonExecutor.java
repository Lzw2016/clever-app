package org.clever.core.job;

import lombok.Getter;
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
public class DaemonExecutor {
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

    public DaemonExecutor(String poolNamePattern) {
        executor = Executors.newSingleThreadScheduledExecutor(
                new BasicThreadFactory.Builder()
                        .namingPattern(poolNamePattern)
                        .daemon(true)
                        .build()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                try {
                    future.cancel(true);
                } catch (Exception ignored) {
                }
            }
            try {
                executor.shutdownNow();
            } catch (Exception ignored) {
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
        future = executor.scheduleAtFixedRate(() -> run(command), 0, period, TimeUnit.MILLISECONDS);
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
            return;
        }
        synchronized (lock) {
            if (running) {
                return;
            }
            running = true;
            try {
                command.run();
            } catch (Exception ignored) {
            } finally {
                running = false;
            }
        }
    }
}
