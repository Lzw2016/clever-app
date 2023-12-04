package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.model.entity.TaskJsJob;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:07 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JsJobModel extends AbstractJob {
    /**
     * 文件内容
     */
    private String content;
    /**
     * 读写权限：0-可读可写，1-只读
     */
    private boolean readOnly;

    public JsJobModel(String name, String content, boolean readOnly) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(content, "参数 content 不能为空");
        this.name = name;
        this.content = content;
        this.readOnly = readOnly;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_3;
    }

    public TaskJsJob toJobEntity() {
        TaskJsJob jsJob = new TaskJsJob();
        jsJob.setContent(getContent());
        jsJob.setReadOnly(isReadOnly() ? EnumConstant.FILE_CONTENT_READ_ONLY_1 : EnumConstant.FILE_CONTENT_READ_ONLY_0);
        return jsJob;
    }
}
