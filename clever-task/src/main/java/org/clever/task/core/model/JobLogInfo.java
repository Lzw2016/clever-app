package org.clever.task.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskJobLog;
import org.clever.task.core.model.entity.TaskJobTriggerLog;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/11 17:14 <br/>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JobLogInfo {
    private TaskJobLog log;
    private TaskJobTriggerLog triggerLog;
    private TaskJob job;
}
