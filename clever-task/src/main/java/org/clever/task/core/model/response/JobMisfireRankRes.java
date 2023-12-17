package org.clever.task.core.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 15:25 <br/>
 */
@Data
public class JobMisfireRankRes {
    /**
     * 错过触发最多的任务
     */
    private List<TaskJob> misfireJobs = new ArrayList<>();
    /**
     * 错过触发最多的任务 {@code Map<jobId, misfireCount>}
     */
    private LinkedHashMap<Long, Integer> misfireJobMap = new LinkedHashMap<>();
}
