package org.clever.core.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.job.GlobalJob;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.util.Assert;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/19 12:46 <br/>
 */
@Slf4j
public class TimedTask {
    private final String name;
    private final Duration interval;
    private final String clazz;
    private final String method;
    private final ClassLoader classLoader;
    private final GlobalJob job;
    private ScheduledFuture<?> future;
    private boolean registeredShutdownHook = false;

    public TimedTask(String name, Duration interval, String clazz, String method, ClassLoader classLoader) {
        Assert.notNull(interval, "参数 interval 不能为null");
        Assert.isTrue(interval.toMillis() > 0, "参数 interval 必须大于0ms");
        Assert.isNotBlank(clazz, "参数 clazz 不能为null");
        Assert.isNotBlank(method, "参数 clazz 不能为null");
        Assert.notNull(classLoader, "参数 classLoader 不能为null");
        this.name = StringUtils.trimToEmpty(name);
        this.interval = interval;
        this.clazz = StringUtils.trimToEmpty(clazz);
        this.method = StringUtils.trimToEmpty(method);
        this.classLoader = classLoader;
        Class<?> cls;
        try {
            cls = classLoader.loadClass(this.clazz);
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.unchecked(e);
        }
        ReflectionsUtils.getStaticMethod(cls, this.method, true);
        this.job = new GlobalJob() {
            @Override
            protected void internalExecute() throws Exception {
                Class<?> clazz = TimedTask.this.classLoader.loadClass(TimedTask.this.clazz);
                Method method = ReflectionsUtils.getStaticMethod(clazz, TimedTask.this.method, true);
                method.invoke(null);
            }

            @Override
            protected void exceptionHandle(Exception e) {
                log.error("执行定时任务失败: [{}]", TimedTask.this.name, e);
            }
        };
    }

    public void start(ScheduledExecutorService scheduled) {
        stop();
        future = scheduled.scheduleAtFixedRate(this.job::execute, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
        if (!registeredShutdownHook) {
            registeredShutdownHook = true;
            AppShutdownHook.addShutdownHook(this::stop, OrderIncrement.NORMAL, "停止开机任务: " + this.name);
        }
    }

    public void stop() {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }
    }
}
