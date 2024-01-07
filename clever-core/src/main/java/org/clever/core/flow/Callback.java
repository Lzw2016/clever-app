package org.clever.core.flow;

import org.clever.core.Ordered;

/**
 * 任务回调，回调逻辑类似:
 * <pre>{@code
 * try {
 *     callback.before(context);
 *     worker.execute(...);
 *     callback.after(context);
 * } finally {
 *     callback.finallyHandle(context);
 * }
 * }</pre>
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
     */
    void after(CallbackContext.After context);

    /**
     * 任务节点调用流程: <br/>
     * <pre>
     * Callback.before
     * 🡓
     * Worker.execute
     * 🡓
     * Callback.after
     * 🡓
     * Callback.finallyHandle
     * </pre>
     * 执行 Callback.before 后<b>一定会执行</b>此函数，当前函数生产的异常<b>不会中断</b>任务链的执行。<br/>
     * 当前函数不应该抛出任何异常，如果抛出了异常，系统会吃掉异常(仅打印异常日志)
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
