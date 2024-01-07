package org.clever.core.flow;

import org.clever.core.Ordered;

/**
 * 任务回调
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:32 <br/>
 */
@FunctionalInterface
public interface Callback extends Ordered {
    /**
     * 任务执行前的回调
     */
    default void before(CallbackContext context) {
    }

    /**
     * 任务执行成功后的回调 <br/>
     * 当前函数异常之后会中断 ??? 调用链
     */
    void after(CallbackContext.After context);

    /**
     * 任务节点调用链: <br/>
     * <pre>
     * Callback.before
     * 🡓
     * Worker.execute
     * 🡓
     * Callback.after
     * 🡓
     * Callback.finallyHandle
     * </pre>
     * 执行 Callback.before 后<b>一定会执行</b>此函数，当前函数生产的异常<b>不会中断</b> ??? 调用链
     */
    default void finallyHandle(CallbackContext.Finally context) {
    }

    /**
     * 任务回调的执行顺序
     *
     * @see org.clever.core.Ordered
     */
    @Override
    default double getOrder() {
        return 0;
    }
}
