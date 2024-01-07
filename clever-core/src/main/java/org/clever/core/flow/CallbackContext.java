package org.clever.core.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.clever.util.Assert;

/**
 * 任务回调上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:56 <br/>
 */
@Data
public class CallbackContext {
    /**
     * 任务上下文
     */
    private final WorkerContext workerContext;
    /**
     * 当前调用的任务节点
     */
    private final WorkerNode worker;
    /**
     * 发起执行当前任务的任务节点(执行入口任务时为null)
     */
    private final WorkerNode from;

    public CallbackContext(WorkerContext workerContext, WorkerNode worker, WorkerNode from) {
        Assert.notNull(workerContext, "参数 workerContext 不能为空");
        Assert.notNull(worker, "参数 worker 不能为空");
        this.workerContext = workerContext;
        this.worker = worker;
        this.from = from;
    }

    @Setter
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class After extends CallbackContext {
        /**
         * Worker.execute 的返回值(可以覆盖返回值)
         */
        private Object result;

        public After(WorkerContext workerContext, WorkerNode worker, WorkerNode from, Object result) {
            super(workerContext, worker, from);
            this.result = result;
        }
    }

    @Setter
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class Finally extends CallbackContext {
        /**
         * Worker.execute 的返回值
         */
        private final Object result;
        /**
         * 执行 Callback.before、Worker.execute、Callback.after 时的异常对象
         */
        private final Throwable err;

        public Finally(WorkerContext workerContext, WorkerNode worker, WorkerNode from, Object result, Throwable err) {
            super(workerContext, worker, from);
            this.result = result;
            this.err = err;
        }
    }
}
