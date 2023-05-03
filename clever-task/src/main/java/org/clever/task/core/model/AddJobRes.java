package org.clever.task.core.model;

import lombok.Data;
import org.clever.task.core.model.entity.*;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/22 20:52 <br/>
 */
@Data
public class AddJobRes implements Serializable {
    private TaskJob job;

    private TaskJobTrigger jobTrigger;

    private TaskHttpJob httpJob ;

    private TaskJavaJob javaJob;

    private TaskJsJob jsJob;

    private TaskShellJob shellJob;
}
