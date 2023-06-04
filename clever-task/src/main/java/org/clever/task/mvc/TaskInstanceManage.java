package org.clever.task.mvc;

import org.clever.core.AppContextHolder;
import org.clever.task.core.TaskInstance;
import org.clever.task.core.model.SchedulerInfo;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/03 22:26 <br/>
 */
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
}
