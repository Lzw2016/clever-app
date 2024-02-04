package org.clever.core.thread;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.util.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程工具类
 * 作者： lzw<br/>
 * 创建时间：2019-01-24 16:15 <br/>
 */
@Slf4j
public class ThreadUtils {
    /**
     * 休眠当前线程，忽略中断异常
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 线程栈信息
     *
     * @return 线程栈信息字符串
     */
    public static String track(Thread thread) {
        if (thread == null) {
            return "";
        }
        StackTraceElement[] stackTrace = thread.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(
                String.format(
                    "%s\tat %s.%s(%s:%s)",
                    System.lineSeparator(),
                    stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getFileName(),
                    stackTraceElement.getLineNumber()
                )
            );
        }
        return stringBuilder.toString();
    }

    /**
     * 当前线程栈信息
     *
     * @return 线程栈信息字符串
     */
    public static String track() {
        return track(Thread.currentThread());
    }

    /**
     * 打印线程栈信息
     */
    public static void printTrack(Thread thread) {
        String str = track(thread);
        if (StringUtils.isBlank(str)) {
            return;
        }
        log.info(str);
    }

    /**
     * 打印当前线程栈信息
     */
    public static void printTrack() {
        printTrack(Thread.currentThread());
    }

    /**
     * 获取线程池状态信息(当前 ExecutorService 不是线程池, 返回 null)
     */
    public static ThreadPoolState getPoolInfo(ExecutorService executorService) {
        Assert.notNull(executorService, "参数 executorService 不能为 null");
        if (!(executorService instanceof ThreadPoolExecutor)) {
            return null;
        }
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
        ThreadPoolState poolState = new ThreadPoolState();
        // 当前活跃执行任务的线程数
        poolState.setActiveCount(pool.getActiveCount());
        // 工作队列中等待执行的任务数
        poolState.setQueueSize(pool.getQueue().size());
        // 当前线程池中的线程数（包括核心线程和非核心线程）
        poolState.setPoolSize(pool.getPoolSize());
        // 是否已调用 shutdown() 方法，不再接受新任务，但会继续处理队列中的任务
        poolState.setShutdown(pool.isShutdown());
        // 线程池是否已经完全终止，即所有提交的任务都已经执行完毕或被取消
        poolState.setTerminated(pool.isTerminated());
        // 已完成的任务总数
        poolState.setCompletedTaskCount(pool.getCompletedTaskCount());
        // 返回计划执行的任务的大致总数。由于任务和线程的状态在计算过程中可能会动态变化，因此返回的值只是一个近似值。
        poolState.setTaskCount(pool.getTaskCount());
        // 核心线程数
        poolState.setCorePoolSize(pool.getCorePoolSize());
        // 最大线程数
        poolState.setMaximumPoolSize(pool.getMaximumPoolSize());
        // 获取拒绝策略等其他详细配置信息
        poolState.setAbortPolicy(pool.getRejectedExecutionHandler().getClass().getSimpleName());
        return poolState;
    }
}
