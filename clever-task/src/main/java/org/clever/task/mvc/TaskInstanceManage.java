package org.clever.task.mvc;

import com.google.common.base.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppContextHolder;
import org.clever.core.exception.BusinessException;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.model.request.page.Page;
import org.clever.core.model.response.R;
import org.clever.task.core.TaskInstance;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.*;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskSchedulerLog;
import org.clever.task.core.model.request.SchedulerLogReq;
import org.clever.task.core.model.request.TaskInfoReq;
import org.clever.task.core.model.request.TaskJobLogReq;
import org.clever.task.core.model.request.TaskJobReq;
import org.clever.validation.annotation.Validated;
import org.clever.web.support.mvc.annotation.RequestBody;
import org.clever.web.support.mvc.annotation.RequestParam;
import org.clever.web.support.mvc.annotation.Transactional;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/03 22:26 <br/>
 */
@Slf4j
public class TaskInstanceManage {
    private static volatile TaskInstance TASK_INSTANCE;

    private static TaskInstance getTaskInstance() {
        if (TASK_INSTANCE == null) {
            TASK_INSTANCE = AppContextHolder.getBean(TaskInstance.class);
        }
        return TASK_INSTANCE;
    }

    /**
     * 获取所有的调度器信息
     */
    @Transactional(disabled = true)
    public static List<SchedulerInfo> allSchedulers() {
        return getTaskInstance().allSchedulers();
    }

    /**
     * 获取所有的 “命名空间”
     */
    @Transactional(disabled = true)
    public static List<String> allNamespace() {
        return getTaskInstance().allNamespace();
    }

    /**
     * 获取所有的 “实例名”
     */
    @Transactional(disabled = true)
    public List<String> allInstance() {
        return getTaskInstance().allInstance();
    }

    /**
     * 获取当前调度器
     */
    @Transactional(disabled = true)
    public static R<?> getCurrentScheduler() {
        return R.success(getTaskInstance().getContext().getCurrentScheduler());
    }

    /**
     * 查询所有任务信息
     */
    @Transactional(disabled = true)
    public static Page<JobInfo> queryJobs(TaskJobReq query) {
        return getTaskInstance().queryJobs(query);
    }

    /**
     * 查询任务详情
     */
    @Transactional(disabled = true)
    public static JobInfo getJob(@RequestParam("id") Long id) {
        return getTaskInstance().getJobInfo(id);
    }

    /**
     * 查询任务详情
     */
    @Transactional(disabled = true)
    public static JobInfo delJob(@RequestParam("id") Long id) {
        return getTaskInstance().deleteJob(id);
    }

    /**
     * 更新任务
     */
    @Transactional(disabled = true)
    public static JobInfo updateJob(@Validated @RequestBody TaskInfoReq req) {
        final TaskInfoReq.TaskJob job = req.getJob();
        final TaskInfoReq.TaskHttpJob http = req.getHttpJob();
        final TaskInfoReq.TaskJavaJob java = req.getJavaJob();
        final TaskInfoReq.TaskJsJob js = req.getJsJob();
        final TaskInfoReq.TaskShellJob shell = req.getShellJob();
        final TaskInfoReq.TaskJobTrigger trigger = req.getJobTrigger();
        AbstractJob jobModel;
        AbstractTrigger jobTrigger;
        if (http != null && http.getId() != null) {
            HttpJobModel httpJob = new HttpJobModel(job.getName(), http.getRequestMethod(), http.getRequestUrl());
            if (StringUtils.isNotBlank(http.getRequestData())) {
                try {
                    httpJob.setRequestData(JacksonMapper.getInstance().fromJson(http.getRequestData(), HttpJobModel.HttpRequestData.class));
                } catch (Exception ignored) {
                }
            }
            httpJob.setSuccessCheck(httpJob.getSuccessCheck());
            jobModel = httpJob;
        } else if (java != null && java.getId() != null) {
            jobModel = new JavaJobModel(
                job.getName(), Objects.equal(java.getIsStatic(), EnumConstant.JAVA_JOB_IS_STATIC_1), java.getClassName(), java.getClassMethod()
            );
        } else if (js != null && js.getId() != null) {
            jobModel = new JsJobModel(
                job.getName(), js.getContent(), Objects.equal(js.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
        } else if (shell != null && shell.getId() != null) {
            ShellJobModel shellJob = new ShellJobModel(
                job.getName(), shell.getShellType(), shell.getContent(), Objects.equal(shell.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
            shellJob.setShellCharset(shell.getShellCharset());
            shellJob.setShellTimeout(shell.getShellTimeout());
            jobModel = shellJob;
        } else {
            throw new BusinessException("任务数据不完整");
        }
        BeanCopyUtils.copyTo(job, jobModel);
        if (Objects.equal(trigger.getType(), EnumConstant.JOB_TRIGGER_TYPE_1)) {
            jobTrigger = new CronTrigger(job.getName(), trigger.getCron());
        } else {
            jobTrigger = new FixedIntervalTrigger(job.getName(), trigger.getFixedInterval());
        }
        BeanCopyUtils.copyTo(trigger, jobTrigger);
        getTaskInstance().addOrUpdateJob(req.getJobId(), jobModel, jobTrigger, req.getNamespace());
        return getTaskInstance().getJobInfo(req.getJobId());
    }

    /**
     * 禁用任务
     */
    @Transactional(disabled = true)
    public static R<?> disableJob(@RequestParam("jobId") Long jobId, @RequestParam("triggerId") Long triggerId) {
        int count = getTaskInstance().disableJob(jobId);
        count += getTaskInstance().disableTrigger(triggerId);
        return R.success(count);
    }

    /**
     * 启用任务
     */
    @Transactional(disabled = true)
    public static R<?> enableJob(@RequestParam("jobId") Long jobId, @RequestParam("triggerId") Long triggerId) {
        int count = getTaskInstance().enableJob(jobId);
        count += getTaskInstance().enableTrigger(triggerId);
        return R.success(count);
    }

    /**
     * 立即执行任务
     */
    @Transactional(disabled = true)
    public static R<?> execJob(@RequestParam("jobId") Long jobId) {
        getTaskInstance().execJob(jobId);
        return R.success();
    }

    /**
     * 获取任务日志信息
     */
    @Transactional(disabled = true)
    public static Page<TaskJobLog> queryTaskJobLog(TaskJobLogReq req) {
        TaskInstance taskInstance = getTaskInstance();
        TaskStore taskStore = taskInstance.getTaskStore();
        return taskStore.beginReadOnlyTX(status -> taskStore.queryTaskJobLog(req));
    }

    /**
     * 获取调度器日志信息
     */
    @Transactional(disabled = true)
    public static Page<TaskSchedulerLog> querySchedulerLog(SchedulerLogReq req) {
        TaskInstance taskInstance = getTaskInstance();
        TaskStore taskStore = taskInstance.getTaskStore();
        return taskStore.beginReadOnlyTX(status -> taskStore.querySchedulerLog(req));
    }
}
