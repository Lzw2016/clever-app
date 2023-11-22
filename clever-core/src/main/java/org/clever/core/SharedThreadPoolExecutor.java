package org.clever.core;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

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
}
