package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.task.core.GlobalConstant;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.entity.TaskJavaJob;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:02 <br/>
 */
@Slf4j
public class JavaJobExecutor implements JobExecutor {
    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>(GlobalConstant.INITIAL_CAPACITY);

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_2);
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) throws Exception {
        final TaskJavaJob javaJob = taskStore.beginReadOnlyTX(status -> taskStore.getJavaJob(scheduler.getNamespace(), job.getId()));
        if (javaJob == null) {
            throw new JobExecutorException(String.format("JavaJob数据不存在，JobId=%s", job.getId()));
        }
        final LinkedHashMap<?, ?> jobData = StringUtils.isBlank(job.getJobData())
                ? new LinkedHashMap<>()
                : JacksonMapper.getInstance().fromJson(job.getJobData(), LinkedHashMap.class);
        final Object[] args = new Object[]{jobData};
        final Class<?> clazz = Class.forName(javaJob.getClassName());
        final String cacheKey = String.format("%s#%s", javaJob.getClassName(), javaJob.getClassMethod());
        Method method = METHOD_CACHE.computeIfAbsent(cacheKey, key -> {
            Method tmp = getAccessibleMethodByName(clazz, javaJob.getClassMethod(), true);
            if (tmp == null) {
                tmp = getAccessibleMethodByName(clazz, javaJob.getClassMethod(), false);
            }
            return tmp;
        });
        if (method == null) {
            throw new JobExecutorException(String.format(
                    "JavaJob执行失败，method不存在，JobId=%s | class=%s | method=%s",
                    job.getId(), javaJob.getClassName(), javaJob.getClassMethod()
            ));
        }
        final boolean hasParameter = method.getParameterTypes().length > 0;
        Object obj = null;
        if (Objects.equals(javaJob.getIsStatic(), EnumConstant.JAVA_JOB_IS_STATIC_0)) {
            obj = clazz.getDeclaredConstructor().newInstance();
        }
        Object res;
        if (hasParameter) {
            res = method.invoke(obj, args);
        } else {
            res = method.invoke(obj);
        }
        log.debug("JavaJob执行完成，class={} | method={} | res={}", javaJob.getClassName(), javaJob.getClassMethod(), res);
        if (hasParameter && Objects.equals(job.getIsUpdateData(), EnumConstant.JOB_IS_UPDATE_DATA_1)) {
            job.setJobData(JacksonMapper.getInstance().toJson(jobData));
        }
    }

    private Method getAccessibleMethodByName(Class<?> searchType, String methodName, boolean hasParameter) {
        while (searchType != Object.class) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (hasParameter) {
                    // 有参数，参数必须是 LinkedHashMap
                    if (parameterTypes.length != 1 || !Map.class.isAssignableFrom(parameterTypes[0])) {
                        continue;
                    }
                } else {
                    // 无参数
                    if (parameterTypes.length > 0) {
                        continue;
                    }
                }
                if (method.getName().equals(methodName)) {
                    // 强制设置方法可以访问(public)
                    makeAccessible(method);
                    return method;
                }
            }
            // 获取父类类型，继续查找方法
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }
}
