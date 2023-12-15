package org.clever.task.core.model.response;

import lombok.Data;
import org.clever.task.core.model.JobLogInfo;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 13:11 <br/>
 */
@Data
public class RunJobsRes {
    /**
     * 最近运行的任务
     */
    private List<JobLogInfo> lastRunJobs;
    /**
     * 正在运行的任务
     */
    private List<JobLogInfo> lastRunningJobs;
    /**
     * 即将运行的任务
     */
    private List<JobLogInfo> waitRunJobs;
}
