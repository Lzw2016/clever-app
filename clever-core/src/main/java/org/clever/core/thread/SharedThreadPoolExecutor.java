package org.clever.core.thread;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        }, OrderIncrement.MAX - 10, "停止SharedThreadPool");
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
                SMALL = ThreadUtils.createThreadPool(
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
                NORMAL = ThreadUtils.createThreadPool(
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
                LARGE = ThreadUtils.createThreadPool(
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
                CACHED_POOL = new ThreadPoolExecutor(
                    0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new BasicThreadFactory.Builder()
                        .namingPattern("cached-shared-%d")
                        .daemon(true)
                        .build(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
                );
            }
        }
        return CACHED_POOL;
    }
}
