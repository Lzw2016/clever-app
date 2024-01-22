package org.clever.task.manage.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 15:33 <br/>
 */
@Data
public class JobRetryRankRes {
    /**
     * 重试次数最多的任务
     */
    private List<TaskJob> maxRetryJobs = new ArrayList<>();
    /**
     * 重试次数最多的任务 {@code Map<jobId, retryCount>}
     */
    private LinkedHashMap<Long, Integer> retryJobMap = new LinkedHashMap<>();
}
