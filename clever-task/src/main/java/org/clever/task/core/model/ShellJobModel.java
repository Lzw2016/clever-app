package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.clever.task.core.model.entity.TaskShellJob;
import org.clever.util.Assert;

import java.util.UUID;

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
     * shell文件内容
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

    public ShellJobModel(String name, String shellType, String filePath, String fileName, String fileContent) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(fileContent, "参数fileContent不能为空");
        this.name = name;
        this.setShellType(shellType);
        this.filePath = StringUtils.isNotBlank(filePath) ? filePath : "/";
        this.fileName = StringUtils.isNotBlank(fileName)
                ? fileName
                : String.format("%s_%s%s", name, UUID.randomUUID(), EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.getOrDefault(shellType, ".txt"));
        this.fileContent = fileContent;
    }

    public ShellJobModel(String name, String shellType, String fileContent) {
        this(name, shellType, null, null, fileContent);
    }

    public void setShellType(String shellType) {
        Assert.hasText(shellType, "参数shellType不能为空");
        Assert.isTrue(EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.containsKey(shellType), "参数shellType值非法，不支持的shellType：" + shellType);
        this.shellType = shellType;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_4;
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

    public TaskShellJob toJobEntity() {
        TaskShellJob shellJob = new TaskShellJob();
        shellJob.setShellType(getShellType());
        return shellJob;
    }
}
