package org.clever.task.core;

/**
 * SQL语句常量
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/03 10:59 <br/>
 */
public interface SqlConstant {
    String DATASOURCE_NOW = "select now(3) from dual";

    // ---------------------------------------------------------------------------------------------------------------------------------------- scheduler

    String GET_SCHEDULER = "select * from scheduler where namespace=? and instance_name=?";

    String ADD_SCHEDULER = "" +
            "insert into scheduler " +
            "(namespace, instance_name, last_heartbeat_time, heartbeat_interval, config, description) " +
            "VALUES " +
            "(:namespace, :instanceName, now(3), :heartbeatInterval, :config, :description)";

    String UPDATE_SCHEDULER = "" +
            "update scheduler " +
            "set last_heartbeat_time=now(3), heartbeat_interval=:heartbeatInterval, config=:config, description=:description " +
            "where namespace=:namespace and instance_name=:instanceName";

    String HEARTBEAT_SCHEDULER = "" +
            "update scheduler " +
            "set last_heartbeat_time=now(3), heartbeat_interval=:heartbeatInterval, config=:config, description=:description " +
            "where namespace=:namespace and instance_name=:instanceName";

    String QUERY_AVAILABLE_SCHEDULER = "" +
            "select * from scheduler " +
            "where namespace=? " +
            "and last_heartbeat_time is not null " +
            "and (heartbeat_interval * 2) > ((unix_timestamp(now(3)) - unix_timestamp(last_heartbeat_time)) * 1000)";

    String QUERY_ALL_SCHEDULER = "" +
            "select " +
            "  *, " +
            "  (heartbeat_interval * 2) > ((unix_timestamp(now(3)) - unix_timestamp(ifnull(last_heartbeat_time, 0))) * 1000) as available " +
            "from scheduler " +
            "where namespace=? ";

    // ---------------------------------------------------------------------------------------------------------------------------------------- job_trigger

    String JOB_TRIGGER_TABLE_NAME = "job_trigger";

    String QUERY_ENABLE_TRIGGER = "select * from job_trigger " +
            "where disable=0 " +
            "and namespace=?";

    String QUERY_ENABLE_CRON_TRIGGER = "select * from job_trigger " +
            "where disable=0 " +
            "and type=1 " +
            "and namespace=?";

    String QUERY_NEXT_TRIGGER = "" +
            "select * from job_trigger " +
            "where disable=0 " +
            "and namespace=? " +
            "and start_time<=now(3) " +
            "and (end_time is null or end_time>=now(3)) " +
            "and next_fire_time is not null " +
            "and unix_timestamp(next_fire_time)-unix_timestamp(now(3))<=? " +
            "order by next_fire_time limit 1200";

    String GET_TRIGGER = "" +
            "select * from job_trigger " +
            "where disable=0 " +
            "and namespace=? " +
            "and id=?";

    // 获取无效的触发器配置数量 -> type=2|3
    String COUNT_INVALID_TRIGGER = "" +
            "select count(1) from job_trigger " +
            "where disable=0 " +
            "and (type=2 and fixed_interval<=0) " +
            "and namespace=?";

    // 更新无效的触发器配置 -> type=2|3 更新 next_fire_time=null
    String UPDATE_INVALID_TRIGGER = "" +
            "update job_trigger set next_fire_time=null " +
            "where disable=0 " +
            "and next_fire_time is not null " +
            "and (type=2 and fixed_interval<=0) " +
            "and namespace=?";

    // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
    String UPDATE_TYPE2_NEXT_FIRE_TIME_TRIGGER = "" +
            "update job_trigger " +
            "set next_fire_time= " +
            "  case " +
            "    when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
            "    when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
            "    when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
            "    else date_add(now(), interval fixed_interval second) " +
            "  end " +
            "where disable=0 " +
            "  and type=2 " +
            "  and fixed_interval>0 " +
            "  and (next_fire_time is null or next_fire_time!= " +
            "    case " +
            "        when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
            "        when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
            "        when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
            "        else date_add(now(), interval fixed_interval second) " +
            "    end) " +
            "  and namespace=?";

    String UPDATE_NEXT_FIRE_TIME_TRIGGER = "" +
            "update job_trigger set next_fire_time=:nextFireTime " +
            "where id=:id " +
            "and namespace=:namespace";

    String UPDATE_FIRE_TIME_TRIGGER = "" +
            "update job_trigger set last_fire_time=:lastFireTime, next_fire_time=:nextFireTime " +
            "where id=:id " +
            "and namespace=:namespace";

    String GET_LOCK_TRIGGER = "update job_trigger set lock_version=lock_version+1 where id=? and namespace=? and lock_version=?";

    String DELETE_TRIGGER_BY_JOB_ID = "delete from job_trigger where namespace=? and job_id=?";

    String UPDATE_DISABLE_TRIGGER = "update job_trigger set disable=? where namespace=? and id in ( %s )";

    // ---------------------------------------------------------------------------------------------------------------------------------------- job

    String JOB_TABLE_NAME = "job";

    String GET_LOCK_JOB = "update job set lock_version=lock_version+1 where id=? and namespace=? and lock_version=?";

    String UPDATE_JOB_DATA = "update job set job_data=? where namespace=? and id=?";

    String GET_JOB_BY_ID = "select * from job where namespace=? and id=?";

    String QUERY_ALL_JOB = "select * from job where namespace=?";

    String UPDATE_DISABLE_JOB = "update job set disable=? where namespace=? and id in ( %s )";

    String DELETE_JOB_BY_JOB_ID = "delete from job where namespace=? and id=?";

    // ---------------------------------------------------------------------------------------------------------------------------------------- xxx_job

    String HTTP_JOB_TABLE_NAME = "http_job";
    String JAVA_JOB_TABLE_NAME = "java_job";
    String JS_JOB_TABLE_NAME = "js_job";
    String SHELL_JOB_TABLE_NAME = "shell_job";

    String HTTP_JOB_BY_JOB_ID = "select * from http_job where namespace=? and job_id=?";

    String JAVA_JOB_BY_JOB_ID = "select * from java_job where namespace=? and job_id=?";

    String JS_JOB_BY_JOB_ID = "select * from js_job where namespace=? and job_id=?";

    String SHELL_JOB_BY_JOB_ID = "select * from shell_job where namespace=? and job_id=?";

    String DELETE_HTTP_JOB_BY_JOB_ID = "delete from http_job where namespace=? and job_id=?";

    String DELETE_JAVA_JOB_BY_JOB_ID = "delete from java_job where namespace=? and job_id=?";

    String DELETE_JS_JOB_BY_JOB_ID = "delete from js_job where namespace=? and job_id=?";

    String DELETE_SHELL_JOB_BY_JOB_ID = "delete from shell_job where namespace=? and job_id=?";

    // ---------------------------------------------------------------------------------------------------------------------------------------- xxx_log

    String ADD_SCHEDULER_LOG = "" +
            "insert into scheduler_log " +
            "(namespace, instance_name, event_name, log_data) " +
            "values " +
            "(:namespace, :instanceName, :eventName, :logData)";

    String ADD_JOB_TRIGGER_LOG = "" +
            "insert into job_trigger_log " +
            "(id, namespace, instance_name, job_trigger_id, job_id, trigger_name, fire_time, is_manual, trigger_time, last_fire_time, next_fire_time, fire_count, mis_fired, trigger_msg) " +
            "values " +
            "(:id, :namespace, :instanceName, :jobTriggerId, :jobId, :triggerName, :fireTime, :isManual, :triggerTime, :lastFireTime, :nextFireTime, :fireCount, :misFired, :triggerMsg)";

    String ADD_JOB_LOG = "" +
            "insert into job_log " +
            "(namespace, instance_name, job_trigger_log_id, job_trigger_id, job_id, fire_time, start_time, retry_count, run_count, before_job_data) " +
            "values " +
            "(:namespace, :instanceName, :jobTriggerLogId, :jobTriggerId, :jobId, :fireTime, now(3), :retryCount, :runCount, :beforeJobData)";

    String UPDATE_JOB_LOG_BY_END = "" +
            "update job_log " +
            "set end_time=now(3), run_time=:runTime, status=:status, exception_info=:exceptionInfo, after_job_data=:afterJobData " +
            "where namespace=:namespace and id=:id";

    String UPDATE_JOB_LOG_BY_RETRY = "" +
            "update job_log " +
            "set run_time=:runTime, status=:status, exception_info=:exceptionInfo, retry_count=:retryCount " +
            "where namespace=:namespace and id=:id";

    // ---------------------------------------------------------------------------------------------------------------------------------------- file_resource

    String FILE_RESOURCE_TABLE_NAME = "file_resource";

    String GET_FILE_RESOURCE_BY_ID = "select * from file_resource where id=? and namespace=?";

    String DELETE_FILE_RESOURCE_BY_ID = "delete from file_resource where namespace=? and id=?";
}
