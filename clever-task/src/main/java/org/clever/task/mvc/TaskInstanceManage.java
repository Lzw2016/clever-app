package org.clever.task.mvc;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.AppContextHolder;
import org.clever.core.model.request.page.Page;
import org.clever.task.core.TaskInstance;
import org.clever.task.core.TaskStore;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskSchedulerLog;
import org.clever.task.core.model.request.SchedulerLogReq;
import org.clever.task.core.model.request.TaskJobLogReq;
import org.clever.task.core.model.request.TaskJobReq;
import org.clever.task.core.model.response.TaskInfoRes;

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
    public static List<SchedulerInfo> allSchedulers() {
        return getTaskInstance().allSchedulers();
    }

    /**
     * 查询所有任务信息
     */
    public static Page<TaskInfoRes> queryJobs(TaskJobReq query) {
        return getTaskInstance().queryJobs(query);
    }

    /**
     * 获取任务日志信息
     */
    public static Page<TaskJobLog> queryTaskJobLog(TaskJobLogReq req) {
        TaskInstance taskInstance = getTaskInstance();
        TaskStore taskStore = taskInstance.getTaskStore();
        return taskStore.beginReadOnlyTX(status -> taskStore.queryTaskJobLog(req));
    }

    /**
     * 获取调度器日志信息
     */
    public static Page<TaskSchedulerLog> querySchedulerLog(SchedulerLogReq req) {
        TaskInstance taskInstance = getTaskInstance();
        TaskStore taskStore = taskInstance.getTaskStore();
        return taskStore.beginReadOnlyTX(status -> taskStore.querySchedulerLog(req));
    }
}
