package org.clever.task.core.model.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器事件日志(task_scheduler_log)
 */
@Data
public class TaskSchedulerLog implements Serializable {
    public static final int LOG_DATA_MAX_LENGTH = 32767;
    /** 数据完整性校验失败 */
    public static final String EVENT_DATA_CHECK_ERROR = "data_check_error";
    /** 调度器节点注册失败 */
    public static final String EVENT_REGISTER_SCHEDULER_ERROR = "register_scheduler_error";
    /** 校准触发器触发时间失败 */
    public static final String EVENT_CALC_NEXT_FIRE_TIME_ERROR = "calc_next_fire_time_error";
    /** 心跳保持失败 */
    public static final String EVENT_HEART_BEAT_ERROR = "heart_beat_error";
    /** 维护当前集群可用的调度器列表失败 */
    public static final String EVENT_RELOAD_SCHEDULER_ERROR = "reload_scheduler_error";
    /** 维护接下来(N+M)秒内需要触发的触发器列表失败 */
    public static final String EVENT_RELOAD_NEXT_TRIGGER_ERROR = "reload_next_trigger_error";
    /** 调度器轮询任务失败 */
    public static final String EVENT_TRIGGER_JOB_EXEC_ERROR = "trigger_job_exec_error";
    /** 校准触发器触发时间过程中，计算cron表达式下次触发时间失败 */
    public static final String EVENT_CALC_CRON_NEXT_FIRE_TIME_ERROR = "calc_cron_next_fire_time_error";
    /** JobTrigger触发失败 */
    public static final String EVENT_TRIGGER_JOB_EXEC_ITEM_ERROR = "trigger_job_exec_item_error";
    /** JobTrigger触发器触发失败 */
    public static final String EVENT_JOB_TRIGGER_FIRE_ERROR = "job_trigger_fire_error";
    /** 调度核心线程错误(会导致调度器暂停) */
    public static final String EVENT_FIRE_TRIGGERS_ERROR = "fire_triggers_error";
    /** 调度器启动成功 */
    public static final String EVENT_STARTED = "started";
    /** 调度器启动失败 */
    public static final String EVENT_STARTED_ERROR = "started_error";
    /** 调度器暂停成功 */
    public static final String EVENT_PAUSED = "paused";
    /** 调度器暂停失败 */
    public static final String EVENT_PAUSED_ERROR = "paused_error";
    /** 调度器继续运行成功 */
    public static final String EVENT_RESUME = "resume";
    /** 调度器继续运行失败 */
    public static final String EVENT_RESUME_ERROR = "resume_error";
    /** 调度器停止 */
    public static final String EVENT_SHUTDOWN = "shutdown";

    /** 编号 */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 调度器实例名称 */
    private String instanceName;
    /** 事件名称 */
    private String eventName;
    /** 事件日志数据 */
    private String logData;
    /** 创建时间 */
    private Date createAt;

    /** 事件日志数据 */
    public void setEventInfo(String eventName, String logData) {
        this.eventName = eventName;
        this.logData = StringUtils.truncate(logData, LOG_DATA_MAX_LENGTH);
    }
}
