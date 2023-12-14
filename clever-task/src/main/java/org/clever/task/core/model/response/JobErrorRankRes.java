package org.clever.task.core.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/14 13:13 <br/>
 */
@Data
public class JobErrorRankRes {
    /**
     * 错过触发最多的任务
     */
    private List<TaskJob> misfireJobs;
    /**
     * 运行失败最多的任务
     */
    private List<TaskJob> failJobs;
    /**
     * 运行耗时最长的任务
     */
    private List<TaskJob> maxRunTimeJobs;
    /**
     * 运行耗时最长的任务
     */
    private List<TaskJob> maxRetryJobs;
}
