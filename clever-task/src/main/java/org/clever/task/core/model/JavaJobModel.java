package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.model.entity.TaskJavaJob;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:07 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JavaJobModel extends AbstractJob {
    /**
     * 是否是静态方法(函数)，0：非静态，1：静态
     */
    private Integer isStatic;
    /**
     * java class全路径
     */
    private String className;

    /**
     * java class method
     */
    private String classMethod;

    public JavaJobModel(String name, boolean isStatic, String className, String classMethod) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(className, "参数className不能为空");
        Assert.hasText(classMethod, "参数classMethod不能为空");
        this.name = name;
        this.isStatic = isStatic ? EnumConstant.JAVA_JOB_IS_STATIC_1 : EnumConstant.JAVA_JOB_IS_STATIC_0;
        this.className = className;
        this.classMethod = classMethod;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_2;
    }

    public TaskJavaJob toJobEntity() {
        TaskJavaJob javaJob = new TaskJavaJob();
        javaJob.setIsStatic(getIsStatic());
        javaJob.setClassName(getClassName());
        javaJob.setClassMethod(getClassMethod());
        return javaJob;
    }
}
