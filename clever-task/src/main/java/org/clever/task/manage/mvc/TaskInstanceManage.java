package org.clever.task.manage.mvc;

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
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.model.*;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskJobTriggerLog;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.task.core.model.entity.TaskSchedulerLog;
import org.clever.task.manage.dao.TaskManageStore;
import org.clever.task.manage.model.JobLogInfo;
import org.clever.task.manage.model.request.*;
import org.clever.task.manage.model.response.*;
import org.clever.validation.annotation.Validated;
import org.clever.web.support.mvc.annotation.RequestBody;
import org.clever.web.support.mvc.annotation.RequestParam;
import org.clever.web.support.mvc.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/03 22:26 <br/>
 */
@Slf4j
public class TaskInstanceManage {
    private static volatile TaskInstance TASK_INSTANCE;
    private static volatile TaskManageStore TASK_MANAGE_STORE;

    private static TaskInstance getTaskInstance() {
        if (TASK_INSTANCE == null) {
            TASK_INSTANCE = AppContextHolder.getBean(TaskInstance.class);
        }
        return TASK_INSTANCE;
    }

    private static TaskManageStore getTaskManageStore() {
        if (TASK_MANAGE_STORE == null) {
            TaskInstance taskInstance = getTaskInstance();
            TASK_MANAGE_STORE = new TaskManageStore(taskInstance.getTaskStore().getQueryDSL());
        }
        return TASK_MANAGE_STORE;
    }

    private static TaskStore getTaskStore() {
        TaskInstance taskInstance = getTaskInstance();
        return taskInstance.getTaskStore();
    }

    /**
     * 验证cron表达式
     */
    @Transactional(disabled = true)
    public static R<?> validateCron(@RequestParam("cron") String cron) {
        return R.create(CronExpressionUtil.isValidExpression(cron), "cron表达式格式正确", "cron表达式格式错误");
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
    public static List<String> allInstance(AllInstanceReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.allInstance(req));
    }

    /**
     * 获取当前调度器
     */
    @Transactional(disabled = true)
    public static TaskScheduler getCurrentScheduler() {
        return getTaskInstance().getContext().getCurrentScheduler();
    }

    /**
     * 暂停调度器
     */
    @Transactional(disabled = true)
    public static R<?> paused(@RequestParam("schedulerId") Long schedulerId) {
        boolean success = getTaskInstance().useCmdPausedScheduler(schedulerId);
        return R.create(success, "操作成功", "操作失败请重试");
    }

    /**
     * 继续运行调度器
     */
    @Transactional(disabled = true)
    public static R<?> resume(@RequestParam("schedulerId") Long schedulerId) {
        boolean success = getTaskInstance().useCmdResumeScheduler(schedulerId);
        return R.create(success, "操作成功", "操作失败请重试");
    }

    /**
     * 查询所有调度器日志信息
     */
    @Transactional(disabled = true)
    public static Page<TaskSchedulerLog> querySchedulerLog(SchedulerLogReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.querySchedulerLog(query));
    }

    /**
     * 查询所有任务信息
     */
    @Transactional(disabled = true)
    public static Page<JobInfo> queryJobs(TaskJobReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.queryJobs(query));
    }

    /**
     * 查询任务详情
     */
    @Transactional(disabled = true)
    public static JobInfo getJob(@RequestParam("id") Long id) {
        TaskStore taskStore = getTaskStore();
        return taskStore.beginReadOnlyTX(status -> taskStore.getJobInfo(id));
    }

    /**
     * 查询任务详情
     */
    @Transactional(disabled = true)
    public static JobInfo delJob(@RequestParam("id") Long id) {
        return getTaskInstance().deleteJob(id);
    }

    /**
     * 新增任务
     */
    @SuppressWarnings("DuplicatedCode")
    @Transactional(disabled = true)
    public static JobInfo addJob(@Validated @RequestBody TaskInfoReq req) {
        final TaskInfoReq.TaskJob job = req.getJob();
        final TaskInfoReq.TaskHttpJob http = req.getHttpJob();
        final TaskInfoReq.TaskJavaJob java = req.getJavaJob();
        final TaskInfoReq.TaskJsJob js = req.getJsJob();
        final TaskInfoReq.TaskShellJob shell = req.getShellJob();
        final TaskInfoReq.TaskJobTrigger trigger = req.getJobTrigger();
        AbstractJob jobModel;
        AbstractTrigger jobTrigger;
        if (Objects.equals(job.getType(), EnumConstant.JOB_TYPE_1)) {
            HttpJobModel httpJob = new HttpJobModel(job.getName(), http.getRequestMethod(), http.getRequestUrl());
            if (StringUtils.isNotBlank(http.getRequestData())) {
                try {
                    httpJob.setRequestData(JacksonMapper.getInstance().fromJson(http.getRequestData(), HttpJobModel.HttpRequestData.class));
                } catch (Exception e) {
                    throw new BusinessException("Http请求数据格式错误", e);
                }
            }
            httpJob.setSuccessCheck(httpJob.getSuccessCheck());
            jobModel = httpJob;
        } else if (Objects.equals(job.getType(), EnumConstant.JOB_TYPE_2)) {
            jobModel = new JavaJobModel(
                job.getName(), Objects.equals(java.getIsStatic(), EnumConstant.JAVA_JOB_IS_STATIC_1), java.getClassName(), java.getClassMethod()
            );
        } else if (Objects.equals(job.getType(), EnumConstant.JOB_TYPE_3)) {
            jobModel = new JsJobModel(
                job.getName(), js.getContent(), Objects.equals(js.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
        } else if (Objects.equals(job.getType(), EnumConstant.JOB_TYPE_4)) {
            ShellJobModel shellJob = new ShellJobModel(
                job.getName(), shell.getShellType(), shell.getContent(), Objects.equals(shell.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
            shellJob.setShellCharset(shell.getShellCharset());
            shellJob.setShellTimeout(shell.getShellTimeout());
            jobModel = shellJob;
        } else {
            throw new BusinessException("任务类型错误type=" + job.getType());
        }
        BeanCopyUtils.copyTo(job, jobModel);
        if (Objects.equals(trigger.getType(), EnumConstant.JOB_TRIGGER_TYPE_1)) {
            jobTrigger = new CronTrigger(job.getName(), trigger.getCron());
        } else {
            jobTrigger = new FixedIntervalTrigger(job.getName(), trigger.getFixedInterval());
        }
        BeanCopyUtils.copyTo(trigger, jobTrigger);
        return getTaskInstance().addJob(jobModel, jobTrigger, req.getNamespace());
    }

    /**
     * 更新任务
     */
    @SuppressWarnings("DuplicatedCode")
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
                } catch (Exception e) {
                    throw new BusinessException("Http请求数据格式错误", e);
                }
            }
            httpJob.setSuccessCheck(httpJob.getSuccessCheck());
            jobModel = httpJob;
        } else if (java != null && java.getId() != null) {
            jobModel = new JavaJobModel(
                job.getName(), Objects.equals(java.getIsStatic(), EnumConstant.JAVA_JOB_IS_STATIC_1), java.getClassName(), java.getClassMethod()
            );
        } else if (js != null && js.getId() != null) {
            jobModel = new JsJobModel(
                job.getName(), js.getContent(), Objects.equals(js.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
        } else if (shell != null && shell.getId() != null) {
            ShellJobModel shellJob = new ShellJobModel(
                job.getName(), shell.getShellType(), shell.getContent(), Objects.equals(shell.getReadOnly(), EnumConstant.FILE_CONTENT_READ_ONLY_1)
            );
            shellJob.setShellCharset(shell.getShellCharset());
            shellJob.setShellTimeout(shell.getShellTimeout());
            jobModel = shellJob;
        } else {
            throw new BusinessException("任务数据不完整");
        }
        BeanCopyUtils.copyTo(job, jobModel);
        if (Objects.equals(trigger.getType(), EnumConstant.JOB_TRIGGER_TYPE_1)) {
            jobTrigger = new CronTrigger(job.getName(), trigger.getCron());
        } else {
            jobTrigger = new FixedIntervalTrigger(job.getName(), trigger.getFixedInterval());
        }
        BeanCopyUtils.copyTo(trigger, jobTrigger);
        return getTaskInstance().addOrUpdateJob(req.getJobId(), jobModel, jobTrigger, req.getNamespace());
    }

    /**
     * 禁用任务
     */
    @Transactional(disabled = true)
    public static R<?> disableJob(@RequestParam("jobId") Long jobId, @RequestParam("triggerId") Long triggerId) {
        long count = getTaskInstance().disableJob(jobId);
        count += getTaskInstance().disableTrigger(triggerId);
        return R.success(count);
    }

    /**
     * 启用任务
     */
    @Transactional(disabled = true)
    public static R<?> enableJob(@RequestParam("jobId") Long jobId, @RequestParam("triggerId") Long triggerId) {
        long count = getTaskInstance().enableJob(jobId);
        count += getTaskInstance().enableTrigger(triggerId);
        return R.success(count);
    }

    /**
     * 立即执行任务
     */
    @Transactional(disabled = true)
    public static R<?> execJob(ExecJobReq req) {
        boolean success = getTaskInstance().useCmdExecJob(req.getJobId(), req.getInstanceName());
        return R.create(success, "操作成功", "操作失败请重试");
    }

    /**
     * 查询所有任务日志信息
     */
    @Transactional(disabled = true)
    public static Page<TaskJobLog> queryTaskJobLog(TaskJobLogReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.queryTaskJobLog(query));
    }

    /**
     * 查询所有任务日志信息
     */
    @Transactional(disabled = true)
    public static Page<JobLogInfo> queryJobLogInfo(JobLogInfoReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.queryJobLogInfo(query));
    }

    /**
     * 查询所有触发器
     */
    @Transactional(disabled = true)
    public static Page<JobInfo> queryTaskJobTriggers(TaskJobTriggerReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.queryTaskJobTriggers(query));
    }

    /**
     * 查询所有触发器日志
     */
    @Transactional(disabled = true)
    public static Page<TaskJobTriggerLog> queryTaskJobTriggerLogs(TaskJobTriggerLogReq query) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.queryTaskJobTriggerLogs(query));
    }

    /**
     * 获取任务对应的触发器日志
     */
    @Transactional(disabled = true)
    public static TaskJobTriggerLog getTaskJobTriggerLog(@RequestParam("jobTriggerLogId") Long jobTriggerLogId) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getTaskJobTriggerLog(jobTriggerLogId));
    }

    /**
     * 获取任务对应的触发器日志
     */
    @Transactional(disabled = true)
    public static StatisticsInfoRes getStatistics() {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getStatistics());
    }

    /**
     * 任务运行统计数据
     */
    @Transactional(disabled = true)
    public static List<CartLineDataRes> getCartLineDataRes(CartLineDataReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getCartLineDataRes(req));
    }

    /**
     * 最近运行的任务
     */
    @Transactional(disabled = true)
    public static List<JobLogInfo> getLastRunJobs(RunJobsReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getLastRunJobs(req));
    }

    /**
     * 正在运行的任务
     */
    @Transactional(disabled = true)
    public static List<JobLogInfo> getLastRunningJobs(RunJobsReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getLastRunningJobs(req));
    }

    /**
     * 即将运行的任务
     */
    @Transactional(disabled = true)
    public static List<JobLogInfo> getWaitRunJobs(RunJobsReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getWaitRunJobs(req));
    }

    /**
     * 错过触发最多的任务
     */
    @Transactional(disabled = true)
    public static JobMisfireRankRes getMisfireJobs(JobErrorRankReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getMisfireJobs(req));
    }

    /**
     * 运行失败最多的任务
     */
    @Transactional(disabled = true)
    public static JobFailRankRes getFailJobs(JobErrorRankReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getFailJobs(req));
    }

    /**
     * 运行耗时最长的任务
     */
    @Transactional(disabled = true)
    public static JobRunTimeRankRes getRunTimeJobs(JobErrorRankReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getRunTimeJobs(req));
    }

    /**
     * 重试最多的任务
     */
    @Transactional(disabled = true)
    public static JobRetryRankRes getRetryJobs(JobErrorRankReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getRetryJobs(req));
    }

    /**
     * 查询任务控制台日志
     */
    @Transactional(disabled = true)
    public static JobConsoleLogRes getJobConsoleLog(JobConsoleLogReq req) {
        TaskManageStore taskManageStore = getTaskManageStore();
        return taskManageStore.beginReadOnlyTX(status -> taskManageStore.getJobConsoleLogs(req));
    }
}
