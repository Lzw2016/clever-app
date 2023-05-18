package org.clever.core.timer;

/**
 * 与 TimerTask 对象是一对一的关系。
 * 与 {@link Timer} 返回的 {@link TimerTask} 关联的对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 15:05 <br/>
 */
public interface Timeout {
    /**
     * 返回创建此对象的 {@link Timer}
     */
    Timer timer();

    /**
     * 返回与此对象关联的 {@link TimerTask}
     */
    TimerTask task();

    /**
     * 当且仅当与此对象关联的 {@link TimerTask} 已过期时，才返回 {@code true}
     */
    boolean isExpired();

    /**
     * 当且仅当与此对象关联的 {@link TimerTask} 已被取消时，返回 {@code true}。
     */
    boolean isCancelled();

    /**
     * 尝试取消与此对象关联的 {@link TimerTask}。如果任务已经执行或取消，它将无副作用地返回。
     *
     * @return 如果取消成功完成则为 true，否则为 false
     */
    boolean cancel();
}
