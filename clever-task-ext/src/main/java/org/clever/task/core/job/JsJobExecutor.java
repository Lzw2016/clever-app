package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.js.api.ScriptEngineInstance;
import org.clever.js.api.pool.EngineInstancePool;
import org.clever.js.graaljs.GraalConstant;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskJsJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.util.Assert;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:55 <br/>
 */
@Slf4j
public class JsJobExecutor implements JobExecutor {
    private final EngineInstancePool<Context, Value> engineInstancePool;

    public JsJobExecutor(EngineInstancePool<Context, Value> engineInstancePool) {
        Assert.notNull(engineInstancePool, "参数 engineInstancePool 不能为null");
        this.engineInstancePool = engineInstancePool;
    }

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_3);
    }

    @Override
    public void exec(final JobContext context) throws Exception {
        final TaskJob job = context.getJob();
        final TaskStore taskStore = context.getTaskStore();
        final TaskScheduler scheduler = context.getScheduler();
        final TaskJsJob jsJob = taskStore.beginReadOnlyTX(status -> taskStore.getJsJob(scheduler.getNamespace(), job.getId()));
        if (jsJob == null) {
            throw new JobExecutorException(String.format("JsJob数据不存在，JobId=%s", job.getId()));
        }
        context.setInnerData(JobContext.INNER_JS_JOB_KEY, jsJob);
        final String jsCode = String.format("(function (){\n %s \n})()", Conv.asString(jsJob.getContent()));
        ScriptEngineInstance<Context, Value> engineInstance = null;
        Context engine = null;
        try {
            engineInstance = engineInstancePool.borrowObject();
            engine = engineInstance.getContext().getEngine();
            engine.enter();
            engine.getBindings(GraalConstant.Js_Language_Id).putMember("context", context);
            // engine.getBindings(GraalConstant.Js_Language_Id).putMember("ThreadUtils", ThreadUtils.class);
            engine.eval(GraalConstant.Js_Language_Id, jsCode);
        } finally {
            if (engine != null) {
                engine.leave();
            }
            if (engineInstance != null) {
                engineInstancePool.returnObject(engineInstance);
            }
        }
    }
}
