package org.clever.context;

/**
 * {@link Lifecycle} 接口的扩展，用于那些需要以特定顺序刷新或关闭时启动的对象。
 *
 * <p>{@link #isAutoStartup()} 返回值指示是否应在上下文刷新时启动此对象。
 * 回调接受 {@link #stop(Runnable)} 方法对于具有异步关闭过程的对象很有用。
 * 此接口的任何实现<i>必须</i>在关闭完成时调用回调的{@code run()}方法，以避免整个{@code ApplicationContext}关闭中不必要的延迟。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 10:22 <br/>
 */
public interface SmartLifecycle extends Lifecycle {
    /**
     * 如果此 {@code Lifecycle} 需要自动启动，则返回 {@code true}。
     * <p>{@code false} 的值表示该组件旨在通过显式 {@link #start()} 调用启动，类似于普通的 {@link Lifecycle} 实现。
     * <p>默认实现返回 {@code true}
     *
     * @see #start()
     */
    default boolean isAutoStartup() {
        return true;
    }

    /**
     * 指示生命周期组件如果当前正在运行则必须停止。
     * <p>默认实现委托给 {@link #stop()} 并立即触发调用线程中的给定回调。
     * 请注意，两者之间没有同步，因此自定义实现可能至少希望将相同的步骤放在它们的公共生命周期监视器（如果有的话）中。
     *
     * @see #stop()
     */
    default void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
