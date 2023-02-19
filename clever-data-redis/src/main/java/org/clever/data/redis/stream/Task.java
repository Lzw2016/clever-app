package org.clever.data.redis.stream;

import java.time.Duration;

/**
 * 在 {@link StreamMessageListenerContainer} 中运行的实际 {@link Task}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 11:01 <br/>
 */
public interface Task extends Runnable, Cancelable {
    /**
     * @return {@literal true} 如果任务当前正在 {@link State#RUNNING running}
     */
    default boolean isActive() {
        return State.RUNNING.equals(getState());
    }

    /**
     * 获取当前生命周期阶段
     *
     * @return 从不为 {@literal null}
     */
    State getState();

    /**
     * 同步<strong>阻塞</strong> 调用，等待此 {@link Task} 变为活动状态。
     * 在 {@link #cancel() cancelling} 之后重新准备开始等待以支持重启。
     *
     * @param timeout 不得为 {@literal null}
     * @return {@code true} 如果任务已经开始。 {@code false} 如果在任务开始之前等待时间已经过去
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    boolean awaitStart(Duration timeout) throws InterruptedException;

    /**
     * {@link Task.State} 定义生命周期阶段实际的 {@link Task}
     */
    enum State {
        CREATED, STARTING, RUNNING, CANCELLED;
    }
}
