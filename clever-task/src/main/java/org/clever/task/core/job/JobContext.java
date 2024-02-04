package org.clever.task.core.job;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.StrFormatter;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.tuples.TupleTwo;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.*;
import org.clever.util.Assert;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/27 17:23 <br/>
 */
@ToString
@EqualsAndHashCode
public class JobContext {
    public static final String INNER_JAVA_JOB_KEY = "inner_java_job_key";
    public static final String INNER_HTTP_JOB_KEY = "inner_http_job_key";
    public static final String INNER_SHELL_JOB_KEY = "inner_shell_job_key";

    /**
     * 数据库的当前时间
     */
    @Getter
    private final Date dbNow;
    /**
     * 当前任务信息
     */
    @Getter
    private final TaskJob job;
    /**
     * 当前任务执行日志
     */
    @Getter
    private final TaskJobLog jobLog;
    /**
     * 调度器信息
     */
    @Getter
    private final TaskScheduler scheduler;
    /**
     * 任务调度模块的 dao 层对象
     */
    @Getter
    private final TaskStore taskStore;
    /**
     * 当前任务数据
     */
    @Getter
    private final LinkedHashMap<String, Object> jobData;
    /**
     * 内部数据
     */
    private final Map<String, Object> innerData = new HashMap<>();
    /**
     * 日志记录器
     */
    private final org.slf4j.Logger logger;
    /**
     * 日志行号
     */
    private final AtomicInteger logLineNum = new AtomicInteger(0);

    public JobContext(Date dbNow, TaskJob job, TaskJobLog jobLog, TaskScheduler scheduler, TaskStore taskStore) {
        Assert.notNull(dbNow, "参数 dbNow 不能为 null");
        Assert.notNull(job, "参数 job 不能为 null");
        Assert.notNull(jobLog, "参数 jobLog 不能为 null");
        Assert.notNull(scheduler, "参数 scheduler 不能为 null");
        Assert.notNull(taskStore, "参数 taskStore 不能为 null");
        this.dbNow = dbNow;
        this.job = job;
        this.jobLog = jobLog;
        this.scheduler = scheduler;
        this.taskStore = taskStore;
        this.jobData = loadJobData(job.getJobData());
        this.logger = LoggerFactory.getLogger(String.format("task.job.%s_%s_%s", job.getNamespace(), job.getName(), job.getId()));
    }

    /**
     * 设置内部数据
     *
     * @param name  名称
     * @param value 值
     */
    void setInnerData(String name, Object value) {
        innerData.put(name, value);
    }

    /**
     * 返回 JavaJob 信息, 当前不是 JavaJob 返回 null
     */
    public TaskJavaJob getJavaJob() {
        return (TaskJavaJob) innerData.get(INNER_JAVA_JOB_KEY);
    }

    /**
     * 返回 HttpJob 信息, 当前不是 HttpJob 返回 null
     */
    public TaskHttpJob getHttpJob() {
        return (TaskHttpJob) innerData.get(INNER_HTTP_JOB_KEY);
    }

    /**
     * 返回 ShellJob 信息, 当前不是 ShellJob 返回 null
     */
    public TaskShellJob getShellJob() {
        return (TaskShellJob) innerData.get(INNER_SHELL_JOB_KEY);
    }

    /**
     * 获取任务数据
     *
     * @param name 名称
     * @param def  默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String name, T def) {
        T data = (T) jobData.get(name);
        if (data == null) {
            data = def;
        }
        return data;
    }

    /**
     * 获取任务数据
     *
     * @param name 名称
     */
    public <T> T getData(String name) {
        return getData(name, null);
    }

    /**
     * 设置任务数据
     *
     * @param name  名称
     * @param value 值
     */
    public void setData(String name, Object value) {
        jobData.put(name, value);
    }

    /**
     * 删除任务数据
     *
     * @param name 名称
     */
    public void removeData(String name) {
        jobData.remove(name);
    }

    /**
     * debug打印输出 (同时将日志写入数据库)
     */
    public void debug(String msg) {
        Object[] args = new Object[]{};
        debug(msg, args);
    }

    /**
     * debug打印输出 (同时将日志写入数据库)
     */
    public void debug(String format, Object... args) {
        if (args == null) {
            args = new Object[]{null};
        }
        TupleTwo<String, Throwable> tuple = logString(format, args);
        if (logger.isDebugEnabled()) {
            if (tuple.getValue2() == null) {
                logger.debug(tuple.getValue1());
            } else {
                logger.debug(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    /**
     * info打印输出 (同时将日志写入数据库)
     */
    public void info(String msg) {
        Object[] args = new Object[]{};
        info(msg, args);
    }

    /**
     * info打印输出 (同时将日志写入数据库)
     */
    public void info(String format, Object... args) {
        if (args == null) {
            args = new Object[]{null};
        }
        TupleTwo<String, Throwable> tuple = logString(format, args);
        if (logger.isInfoEnabled()) {
            if (tuple.getValue2() == null) {
                logger.info(tuple.getValue1());
            } else {
                logger.info(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    /**
     * warn打印输出 (同时将日志写入数据库)
     */
    public void warn(String msg) {
        Object[] args = new Object[]{};
        warn(msg, args);
    }

    /**
     * warn打印输出 (同时将日志写入数据库)
     */
    public void warn(String format, Object... args) {
        if (args == null) {
            args = new Object[]{null};
        }
        TupleTwo<String, Throwable> tuple = logString(format, args);
        if (logger.isWarnEnabled()) {
            if (tuple.getValue2() == null) {
                logger.warn(tuple.getValue1());
            } else {
                logger.warn(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    /**
     * error打印输出 (同时将日志写入数据库)
     */
    public void error(String msg) {
        Object[] args = new Object[]{};
        error(msg, args);
    }

    /**
     * error打印输出 (同时将日志写入数据库)
     */
    public void error(String format, Object... args) {
        if (args == null) {
            args = new Object[]{null};
        }
        TupleTwo<String, Throwable> tuple = logString(format, args);
        if (logger.isErrorEnabled()) {
            if (tuple.getValue2() == null) {
                logger.error(tuple.getValue1());
            } else {
                logger.error(tuple.getValue1(), tuple.getValue2());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, Object> loadJobData(String jobData) {
        return StringUtils.isBlank(jobData) ? new LinkedHashMap<>() : JacksonMapper.getInstance().fromJson(jobData, LinkedHashMap.class);
    }

    /**
     * 根据日志输出参数得到日志字符串
     */
    protected TupleTwo<String, Throwable> logString(String format, Object... args) {
        TupleTwo<String, Throwable> res;
        if (args == null || args.length == 0) {
            res = TupleTwo.creat(format, null);
            res.setValue1(format);
        } else {
            Throwable throwable = null;
            if (args[args.length - 1] instanceof Throwable) {
                throwable = (Throwable) args[args.length - 1];
            }
            String logsText;
            if (throwable == null) {
                logsText = StrFormatter.format(format, args);
            } else {
                int length = args.length - 1;
                Object[] array = new Object[length];
                System.arraycopy(args, 0, array, 0, length);
                logsText = StrFormatter.format(format, array);
            }
            res = TupleTwo.creat(logsText, throwable);
        }
        // 将打印任务日志保存到数据库表中
        TaskJobConsoleLog jobConsoleLog = new TaskJobConsoleLog();
        jobConsoleLog.setId(taskStore.getSnowFlake().nextId());
        jobConsoleLog.setNamespace(jobLog.getNamespace());
        jobConsoleLog.setInstanceName(jobLog.getInstanceName());
        jobConsoleLog.setJobId(jobLog.getJobId());
        jobConsoleLog.setJobLogId(jobLog.getId());
        jobConsoleLog.setLineNum(logLineNum.incrementAndGet());
        jobConsoleLog.setLog(res.getValue1() + ExceptionUtils.getStackTraceAsString(res.getValue2()));
        jobConsoleLog.setCreateAt(new Date());
        taskStore.newBeginTX(status -> taskStore.saveJobConsoleLog(jobConsoleLog));
        return res;
    }
}
