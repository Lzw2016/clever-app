package org.clever.core;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.util.Assert;

import java.util.concurrent.*;

/**
 * 全局共享的线程池
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 15:17 <br/>
 */
public class SharedThreadPoolExecutor {
    private static ThreadPoolExecutor SMALL;
    private static final Object LOCK_SMALL = new Object();
    private static ThreadPoolExecutor NORMAL;
    private static final Object LOCK_NORMAL = new Object();
    private static ThreadPoolExecutor LARGE;
    private static final Object LOCK_LARGE = new Object();
    private static ThreadPoolExecutor CACHED_POOL;
    private static final Object LOCK_CACHED_POOL = new Object();

    static {
        AppShutdownHook.addShutdownHook(() -> {
            if (SMALL != null) {
                SMALL.shutdownNow();
            }
            if (NORMAL != null) {
                NORMAL.shutdownNow();
            }
            if (LARGE != null) {
                LARGE.shutdownNow();
            }
            if (CACHED_POOL != null) {
                CACHED_POOL.shutdownNow();
            }
        }, OrderIncrement.MAX - 1, "停止SharedThreadPool");
    }

    private static ThreadPoolExecutor createThreadPool(int core, int max, BlockingQueue<Runnable> queue, String namingPattern) {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            core, max, 16, TimeUnit.SECONDS,
            queue,
            new BasicThreadFactory.Builder()
                .namingPattern(namingPattern)
                .daemon(true)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        threadPool.allowCoreThreadTimeOut(true);
        return threadPool;
    }

    /**
     * 较小的连接池 <br />
     * <pre>{@code
     * corePoolSize     = 2
     * maximumPoolSize  = 32
     * workQueue        = new SynchronousQueue<>()
     * }</pre>
     */
    public static ThreadPoolExecutor getSmall() {
        synchronized (LOCK_SMALL) {
            if (SMALL == null) {
                SMALL = createThreadPool(
                    2,
                    32,
                    new SynchronousQueue<>(),
                    "small-shared-%d"
                );
            }
        }
        return SMALL;
    }

    /**
     * 正常大小的连接池 <br />
     * <pre>{@code
     * corePoolSize     = 16
     * maximumPoolSize  = 32
     * workQueue        = new ArrayBlockingQueue<>(64)
     * }</pre>
     */
    public static ThreadPoolExecutor getNormal() {
        synchronized (LOCK_NORMAL) {
            if (NORMAL == null) {
                NORMAL = createThreadPool(
                    16,
                    32,
                    new ArrayBlockingQueue<>(64),
                    "normal-shared-%d"
                );
            }
        }
        return NORMAL;
    }

    /**
     * 较大的连接池 <br />
     * <pre>{@code
     * corePoolSize     = 512
     * maximumPoolSize  = 1024
     * workQueue        = new ArrayBlockingQueue<>(20480)
     * }</pre>
     */
    public static ThreadPoolExecutor getLarge() {
        synchronized (LOCK_LARGE) {
            if (LARGE == null) {
                LARGE = createThreadPool(
                    512,
                    1024,
                    new ArrayBlockingQueue<>(20480),
                    "large-shared-%d"
                );
            }
        }
        return LARGE;
    }

    /**
     * 获取一个无界的的线程池，与 Executors.newCachedThreadPool() 相同，适合大量短生命周期的任务。<br/>
     * <pre>
     * 线程数量无界:
     *  即最大线程数是 Integer.MAX_VALUE
     * 线程复用与回收:
     *  当有任务提交到线程池时，如果当前线程池中没有空闲线程，则会创建新的线程来执行任务。
     *  当一个线程完成其任务后，它不会立即被销毁，而是等待一段时间看看是否有新的任务可以处理。
     *  如果线程在指定的时间段内一直是空闲状态，那么这个空闲线程可能会被终止以释放系统资源。
     * </pre>
     */
    public static ThreadPoolExecutor getCachedPool() {
        synchronized (LOCK_CACHED_POOL) {
            if (CACHED_POOL == null) {
                CACHED_POOL = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
            }
        }
        return CACHED_POOL;
    }

    /**
     * 获取线程池状态信息
     */
    public static String getPoolInfo(ThreadPoolExecutor pool) {
        Assert.notNull(pool, "参数 pool 不能为 null");
        // 当前活跃执行任务的线程数
        int activeCount = pool.getActiveCount();
        // 工作队列中等待执行的任务数
        int queueSize = pool.getQueue().size();
        // 当前线程池中的线程数（包括核心线程和非核心线程）
        int poolSize = pool.getPoolSize();
        // 是否已调用 shutdown() 方法，不再接受新任务，但会继续处理队列中的任务
        boolean isShutdown = pool.isShutdown();
        // 线程池是否已经完全终止，即所有提交的任务都已经执行完毕或被取消
        boolean isTerminated = pool.isTerminated();
        // 已完成的任务总数
        long completedTaskCount = pool.getCompletedTaskCount();
        // 返回计划执行的任务的大致总数。由于任务和线程的状态在计算过程中可能会动态变化，因此返回的值只是一个近似值。
        long taskCount = pool.getTaskCount();
        // 核心线程数
        int corePoolSize = pool.getCorePoolSize();
        // 最大线程数
        int maximumPoolSize = pool.getMaximumPoolSize();
        // 获取拒绝策略等其他详细配置信息
        String abortPolicy = pool.getRejectedExecutionHandler().getClass().getSimpleName();
        return "activeCount=" + activeCount +
            ", queueSize=" + queueSize
            + ", poolSize=" + poolSize
            + ", isShutdown=" + isShutdown
            + ", isTerminated=" + isTerminated
            + ", completedTaskCount=" + completedTaskCount
            + ", taskCount=" + taskCount
            + ", corePoolSize=" + corePoolSize
            + ", maximumPoolSize=" + maximumPoolSize
            + ", abortPolicy=" + abortPolicy;
    }
}
