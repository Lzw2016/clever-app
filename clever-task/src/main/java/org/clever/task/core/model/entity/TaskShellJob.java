package org.clever.task.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * js脚本任务(task_shell_job)
 */
@Data
public class TaskShellJob implements Serializable {
    /** 主键id */
    private Long id;
    /** 命名空间 */
    private String namespace;
    /** 任务ID */
    private Long jobId;
    /** shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php */
    private String shellType;
    /** 文件内容 */
    private String content;
    /** 读写权限：0-可读可写，1-只读 */
    private Byte readOnly;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
