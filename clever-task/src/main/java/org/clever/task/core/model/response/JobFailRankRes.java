package org.clever.task.core.model.response;

import lombok.Data;
import org.clever.task.core.model.entity.TaskJob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 15:28 <br/>
 */
@Data
public class JobFailRankRes {
    /**
     * 运行失败最多的任务
     */
    private List<TaskJob> failJobs = new ArrayList<>();
    /**
     * 运行失败最多的任务 {@code Map<jobId, failCount>}
     */
    private LinkedHashMap<Long, Integer> failJobMap = new LinkedHashMap<>();
}
