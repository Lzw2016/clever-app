package org.clever.context;

/**
 * 用于 start/stop 生命周期控制的通用接口定义方法。
 * 典型的用例是控制异步处理。
 * <b>注意：此接口并不暗示特定的自动启动语义。考虑为此目的实施 {@link SmartLifecycle}。</b>
 *
 * <p>可以由组件（通常是在 Spring 上下文中定义的 Spring bean）和容器（通常是 Spring {@code ApplicationContext} itself）实现。
 * 容器会将 start/stop 信号传播到每个容器内应用的所有组件，例如对于运行时的 stop/restart 场景。
 *
 * <p>可用于直接调用或通过 JMX 进行管理操作。
 * 在后一种情况下，通常使用 {@code MBeanExporter} 定义 {@code InterfaceBasedMBeanInfoAssembler}，将活动控制组件的可见性限制为生命周期接口。
 *
 * <p>请注意，目前的 {@code Lifecycle} 接口仅在<b>顶级单例 bean 上受支持</b>.
 * 在任何其他组件上，{@code Lifecycle} 接口将保持未检测到并因此被忽略。
 * 另请注意，扩展的 {@link SmartLifecycle} 接口提供了与应用程序上下文的启动和关闭阶段的复杂集成。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/18 11:36 <br/>
 *
 * @see SmartLifecycle
 */
public interface Lifecycle {
    /**
     * 启动这个组件。
     * <p>如果组件已经在运行，则不应抛出异常。
     * <p>在容器的情况下，这会将启动信号传播到所有适用的组件。
     *
     * @see SmartLifecycle#isAutoStartup()
     */
    void start();

    /**
     * 停止此组件，通常以同步方式停止，以便组件在此方法返回时完全停止。
     * 当需要异步停止行为时，考虑实施 {@link SmartLifecycle} 及其 {@code stop(Runnable)} 变体。
     * <p>请注意，此停止通知不能保证在销毁之前到达：在定期关闭时，{@code Lifecycle} beans 将在传播一般销毁回调之前首先收到停止通知；
     * 然而，在上下文生命周期内的热刷新或刷新尝试中止时，将调用给定 bean 的 destroy 方法，而无需预先考虑停止信号。
     * <p>如果组件未运行（尚未启动），不应抛出异常。
     * <p>在容器的情况下，这会将停止信号传播到所有适用的组件。
     *
     * @see SmartLifecycle#stop(Runnable)
     * @see org.clever.beans.factory.DisposableBean#destroy()
     */
    void stop();

    /**
     * 检查此组件当前是否正在运行。
     * <p>在容器的情况下，仅当 <i>all</i> 应用的组件当前正在运行时，它才会返回 {@code true}
     *
     * @return 组件当前是否正在运行
     */
    boolean isRunning();
}
