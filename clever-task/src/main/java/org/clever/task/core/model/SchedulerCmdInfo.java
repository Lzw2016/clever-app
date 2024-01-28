package org.clever.task.core.model;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/28 21:22 <br/>
 */
@Data
public class SchedulerCmdInfo {
    /**
     * 执行任务
     */
    public static final String EXEC_JOB = "execJob";

    /**
     * 需要执行的操作
     */
    private String operation;
    /**
     * 任务 ID
     */
    private Long jobId;

    public static SchedulerCmdInfo createExecJobCmd(Long jobId) {
        SchedulerCmdInfo cmdInfo = new SchedulerCmdInfo();
        cmdInfo.operation = EXEC_JOB;
        cmdInfo.jobId = jobId;
        return cmdInfo;
    }
}
