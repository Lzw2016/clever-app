package org.clever.core.timer;

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度器。
 * 调度 {@link TimerTask} 在后台线程中一次性执行。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 15:04 <br/>
 */
public interface Timer {
    /**
     * 调度指定的 {@link TimerTask} 在指定的延迟后一次性执行
     *
     * @param task  后台任务
     * @param delay 延迟时间
     * @param unit  延迟时间单位
     * @return 与指定任务关联的对象
     * @throws IllegalStateException      如果这个计时器已经{@linkplain #stop() stop}了
     * @throws RejectedExecutionException 如果挂起的超时太多并且创建新的超时可能会导致系统不稳定
     */
    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

    /**
     * 释放此 {@link Timer} 获取的所有资源，并取消所有已调度但尚未执行的任务。
     *
     * @return 与此方法取消的任务关联的对象
     */
    Set<Timeout> stop();

    /**
     * 调度器是否停止
     */
    boolean isStop();
}
