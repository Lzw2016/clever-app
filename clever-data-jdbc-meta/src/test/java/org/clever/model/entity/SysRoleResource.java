package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色资源关联表(sys_role_resource)
 */
@Data
public class SysRoleResource implements Serializable {
    /** 角色id */
    private Long roleId;
    /** 资源id */
    private Long resourceId;
    /** 创建人(用户id) */
    private Long createBy;
    /** 创建时间 */
    private Date createAt;
    /** 更新人(用户id) */
    private Long updateBy;
    /** 更新时间 */
    private Date updateAt;
}
