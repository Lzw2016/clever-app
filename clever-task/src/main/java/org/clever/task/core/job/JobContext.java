package org.clever.task.core.job;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.entity.*;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/27 17:23 <br/>
 */
@Data
public class JobContext {
    public static final String INNER_JAVA_JOB_KEY = "inner_java_job_key";
    public static final String INNER_HTTP_JOB_KEY = "inner_http_job_key";
    public static final String INNER_SHELL_JOB_KEY = "inner_shell_job_key";

    /**
     * 数据库的当前时间
     */
    private final Date dbNow;
    /**
     * 当前任务信息
     */
    private final TaskJob job;
    /**
     * 调度器信息
     */
    private final TaskScheduler scheduler;
    /**
     * 任务调度模块的 dao 层对象
     */
    private final TaskStore taskStore;
    /**
     * 当前任务数据
     */
    private final LinkedHashMap<String, Object> jobData;
    /**
     * 内部数据
     */
    private final Map<String, Object> innerData = new HashMap<>();

    public JobContext(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) {
        this.dbNow = dbNow;
        this.job = job;
        this.scheduler = scheduler;
        this.taskStore = taskStore;
        this.jobData = loadJobData(job.getJobData());
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

    // TODO 支持打印任务日志到 数据库表中

    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, Object> loadJobData(String jobData) {
        return StringUtils.isBlank(jobData) ? new LinkedHashMap<>() : JacksonMapper.getInstance().fromJson(jobData, LinkedHashMap.class);
    }
}
