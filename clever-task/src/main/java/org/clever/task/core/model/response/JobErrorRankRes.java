package org.clever.task.core.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private List<TaskJob> misfireJobs = new ArrayList<>();
    /**
     * 错过触发最多的任务 {@code Map<jobId, misfireCount>}
     */
    private LinkedHashMap<Long, Integer> misfireJobMap = new LinkedHashMap<>();
    /**
     * 运行失败最多的任务
     */
    private List<TaskJob> failJobs = new ArrayList<>();
    /**
     * 运行失败最多的任务 {@code Map<jobId, failCount>}
     */
    private LinkedHashMap<Long, Integer> failJobMap = new LinkedHashMap<>();
    /**
     * 运行耗时最长的任务
     */
    private List<TaskJob> maxRunTimeJobs = new ArrayList<>();
    /**
     * 运行耗时最长的任务 {@code Map<jobId, sumRunTime>}
     */
    private LinkedHashMap<Long, Integer> sumRunTimeJobMap = new LinkedHashMap<>();
    /**
     * 重试次数最多的任务
     */
    private List<TaskJob> maxRetryJobs = new ArrayList<>();
    /**
     * 重试次数最多的任务 {@code Map<jobId, retryCount>}
     */
    private LinkedHashMap<Long, Integer> retryJobMap = new LinkedHashMap<>();
}
