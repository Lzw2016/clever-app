package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 资源表(sys_resource)
 */
@Data
public class SysResource implements Serializable {
    /** 资源id */
    private Long id;
    /** 权限编码 */
    private String permission;
    /** 资源类型: 1:API权限，2:菜单权限，3:UI权限(如:按钮、表单、表格) */
    private Integer resourceType;
    /** 是否启用: 0:禁用，1:启用 */
    private Integer isEnable;
    /** 创建人(用户id) */
    private Long createBy;
    /** 创建时间 */
    private Date createAt;
    /** 更新人(用户id) */
    private Long updateBy;
    /** 更新时间 */
    private Date updateAt;
}
