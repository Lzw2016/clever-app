package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.clever.task.core.model.entity.TaskJsJob;
import org.clever.util.Assert;

import java.util.UUID;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:07 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JsJobModel extends AbstractJob {
    /**
     * js文件内容
     */
    private String fileContent;
    /**
     * 文件路径(以"/"结束)
     */
    private String filePath;
    /**
     * 文件名称
     */
    private String fileName;

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_3;
    }

    public JsJobModel(String name, String filePath, String fileName, String fileContent) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(fileContent, "参数fileContent不能为空");
        this.name = name;
        this.filePath = StringUtils.isNotBlank(filePath) ? filePath : "/";
        this.fileName = StringUtils.isNotBlank(fileName) ? fileName : String.format("%s_%s.js", name, UUID.randomUUID());
        this.fileContent = fileContent;
    }

    public JsJobModel(String name, String fileContent) {
        this(name, null, null, fileContent);
    }

//    @SuppressWarnings("DuplicatedCode")
//    public FileResource toFileResource() {
//        FileResource fileResource = new FileResource();
//        fileResource.setModule(EnumConstant.FILE_RESOURCE_MODULE_4);
//        fileResource.setPath(getFilePath());
//        fileResource.setName(getFileName());
//        fileResource.setContent(getFileContent());
//        fileResource.setIsFile(EnumConstant.FILE_RESOURCE_IS_FILE_1);
//        fileResource.setReadOnly(EnumConstant.FILE_RESOURCE_READ_ONLY_0);
//        fileResource.setDescription(getDescription());
//        return fileResource;
//    }

    public TaskJsJob toJobEntity() {
        return new TaskJsJob();
    }
}
