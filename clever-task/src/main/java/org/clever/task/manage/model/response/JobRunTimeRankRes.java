package org.clever.task.manage.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 15:30 <br/>
 */
@Data
public class JobRunTimeRankRes {
    /**
     * 运行耗时最长的任务
     */
    private List<TaskJob> maxRunTimeJobs = new ArrayList<>();
    /**
     * 运行耗时最长的任务 {@code Map<jobId, sumRunTime>}
     */
    private LinkedHashMap<Long, Integer> avgRunTimeJobMap = new LinkedHashMap<>();
}
