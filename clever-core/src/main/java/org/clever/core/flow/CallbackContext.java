package org.clever.core.flow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务回调上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 12:56 <br/>
 */
@Data
public class CallbackContext {
//    private final WorkerContext workerContext;
    private final Worker worker;

    public CallbackContext(Worker worker) {
        this.worker = worker;
    }

    @Setter
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class After extends CallbackContext {
        /**
         * Worker.execute 的返回值(可以覆盖返回值)
         */
        private Object result;

        public After(Worker worker) {
            super(worker);
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

        public Finally(Worker worker, Object result, Throwable err) {
            super(worker);
            this.result = result;
            this.err = err;
        }
    }
}
