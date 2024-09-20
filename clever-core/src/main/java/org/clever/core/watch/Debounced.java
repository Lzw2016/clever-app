package org.clever.core.watch;

import org.clever.core.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 13:40 <br/>
 */
public class Debounced<T> {
    /**
     * 线程调度器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    /**
     * 实际需要调用的操作
     */
    private final Consumer<T> callback;
    /**
     * 延时时间(单位毫秒)
     */
    private final long delayMillis;
    /**
     * 并发锁
     */
    private final Object lock = new Object();
    /**
     * 调用操作时的参数
     */
    private T arg;
    /**
     * 最后执行时间
     */
    private long lastRunTime = -1;

    /**
     * @param callback    实际需要调用的操作
     * @param delayMillis 延时时间(单位毫秒)
     */
    public Debounced(Consumer<T> callback, long delayMillis) {
        Assert.notNull(callback, "参数 operation 不能为  null");
        Assert.isTrue(delayMillis >= 10, "参数 delayMillis 必须大于等于10");
        this.callback = callback;
        this.delayMillis = delayMillis;
    }

    /**
     * 以debounce的方式执行操作
     */
    public void execute(final T arg) {
        final long currentTime = System.currentTimeMillis();
        // 设置执行参数
        synchronized (lock) {
            this.arg = arg;
        }
        if (currentTime >= lastRunTime) {
            lastRunTime = currentTime + delayMillis;
            // 需要设置执行计划
            scheduler.schedule(() -> {
                synchronized (lock) {
                    callback.accept(this.arg);
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 释放资源(线程池)
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
