package org.clever.task.core.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.clever.task.core.model.entity.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/07/11 21:24 <br/>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskInfoRes {
    private TaskJob job;
    private TaskHttpJob httpJob;
    private TaskJavaJob javaJob;
    private TaskJsJob jsJob;
    private TaskShellJob shellJob;
    private TaskJobTrigger jobTrigger;
}
