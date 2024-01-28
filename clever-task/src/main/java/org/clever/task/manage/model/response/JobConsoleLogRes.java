package org.clever.task.manage.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJobConsoleLog;
import org.clever.task.core.model.entity.TaskJobLog;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/28 15:10 <br/>
 */
@Data
public class JobConsoleLogRes {
    /**
     * 任务日志
     */
    private TaskJobLog jobLog;
    /**
     * 任务控制台日志
     */
    private List<TaskJobConsoleLog> jobConsoleLogs;
}
