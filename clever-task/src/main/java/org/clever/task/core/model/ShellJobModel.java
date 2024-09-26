package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.core.Assert;
import org.clever.task.core.model.entity.TaskShellJob;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:07 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ShellJobModel extends AbstractJob {
    /**
     * shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php
     */
    private String shellType;
    /**
     * 执行终端的字符集编码，如：“UTF-8”
     */
    private String shellCharset;
    /**
     * 执行超时时间，单位：秒，默认：“10分钟”
     */
    private Integer shellTimeout;
    /**
     * 文件内容
     */
    private String content;
    /**
     * 读写权限：0-可读可写，1-只读
     */
    private boolean readOnly;

    public ShellJobModel(String name, String shellType, String content, boolean readOnly) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(content, "参数 content 不能为空");
        this.name = name;
        this.setShellType(shellType);
        this.content = content;
        this.readOnly = readOnly;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_4;
    }

    public void setShellType(String shellType) {
        Assert.hasText(shellType, "参数shellType不能为空");
        Assert.isTrue(EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.containsKey(shellType), "参数shellType值非法，不支持的shellType：" + shellType);
        this.shellType = shellType;
    }

    public TaskShellJob toJobEntity() {
        TaskShellJob shellJob = new TaskShellJob();
        shellJob.setShellType(getShellType());
        shellJob.setShellCharset(getShellCharset());
        shellJob.setShellTimeout(getShellTimeout());
        shellJob.setContent(getContent());
        shellJob.setReadOnly(isReadOnly() ? EnumConstant.FILE_CONTENT_READ_ONLY_1 : EnumConstant.FILE_CONTENT_READ_ONLY_0);
        return shellJob;
    }
}
