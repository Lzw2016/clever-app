package org.clever.core.timer;

import java.util.concurrent.TimeUnit;

/**
 * 定时任务，所有的定时任务都要继承该接口。
 * 在 {@link Timer#newTimeout(TimerTask, long, TimeUnit)} 指定的延迟后执行的任务。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 15:15 <br/>
 */
@FunctionalInterface
public interface TimerTask {
    /**
     * 在指定的延迟后执行 {@link Timer#newTimeout(TimerTask, long, TimeUnit)}
     *
     * @param timeout 与此任务关联的 {@link Timeout} 对象
     */
    void run(Timeout timeout) throws Exception;
}
