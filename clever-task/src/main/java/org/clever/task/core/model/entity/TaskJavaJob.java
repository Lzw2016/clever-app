package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * js脚本任务(task_java_job)
 */
@Data
public class TaskJavaJob implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 任务ID */
    private Long jobId;
    /** 是否是静态方法(函数)，0：非静态，1：静态 */
    private Byte isStatic;
    /** java class全路径 */
    private String className;
    /** java class method */
    private String classMethod;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
