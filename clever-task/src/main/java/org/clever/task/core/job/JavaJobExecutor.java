package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.entity.TaskJavaJob;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.support.ClassMethodLoader;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:02 <br/>
 */
@Slf4j
public class JavaJobExecutor implements JobExecutor {
    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_2);
    }

    @Override
    public void exec(final JobContext context) throws Exception {
        final TaskJob job = context.getJob();
        final TaskStore taskStore = context.getTaskStore();
        final TaskScheduler scheduler = context.getScheduler();
        final TaskJavaJob javaJob = taskStore.beginReadOnlyTX(status -> taskStore.getJavaJob(scheduler.getNamespace(), job.getId()));
        if (javaJob == null) {
            throw new JobExecutorException(String.format("JavaJob数据不存在，JobId=%s", job.getId()));
        }
        context.setInnerData(JobContext.INNER_JAVA_JOB_KEY, javaJob);
        TupleTwo<Class<?>, Method> tuple = ClassMethodLoader.getMethod(javaJob.getClassName(), javaJob.getClassMethod());
        if (tuple == null) {
            throw new JobExecutorException(String.format(
                "JavaJob执行失败，method不存在，JobId=%s | class=%s | method=%s",
                job.getId(), javaJob.getClassName(), javaJob.getClassMethod()
            ));
        }
        final boolean hasParameter = tuple.getValue2().getParameterTypes().length > 0;
        Object obj = null;
        if (Objects.equals(javaJob.getIsStatic(), EnumConstant.JAVA_JOB_IS_STATIC_0)) {
            obj = tuple.getValue1().getDeclaringClass().getDeclaredConstructor().newInstance();
        }
        Object res;
        if (hasParameter) {
            res = tuple.getValue2().invoke(obj, context);
        } else {
            res = tuple.getValue2().invoke(obj);
        }
        log.debug("JavaJob执行完成，class={} | method={} | res={}", javaJob.getClassName(), javaJob.getClassMethod(), res);
    }
}
