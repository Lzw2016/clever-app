package org.clever.data.redis.stream;

import java.time.Duration;

/**
 * {@link Subscription} 是指向实际运行的{@link Task} 的链接。
 * <p>
 * 由于 {@link Task} 执行的异步性质，{@link Subscription} 可能不会立即变为活动状态。
 * 如果底层 {@link Task} 已经在运行，{@link #isActive()} 会提供一个答案。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 10:43 <br/>
 */
public interface Subscription extends Cancelable {
    /**
     * @return {@literal true} 如果当前正在执行订阅
     */
    boolean isActive();

    /**
     * 一旦 {@link Subscription} 变为 {@link #isActive() active} 或 {@link Duration timeout} 超过，同步、<strong>阻塞</strong> 调用返回
     *
     * @param timeout 不得为 {@literal null}
     * @return {@code true} 如果订阅被激活。 {@code false} 如果在激活任务之前等待时间已经过去
     * @throws InterruptedException 如果当前线程在等待时被中断
     */
    boolean await(Duration timeout) throws InterruptedException;
}
