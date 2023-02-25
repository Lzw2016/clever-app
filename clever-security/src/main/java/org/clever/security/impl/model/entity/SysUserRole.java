package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户角色关联表(sys_user_role)
 */
@Data
public class SysUserRole implements Serializable {
    /** 用户id */
    private Long userId;
    /** 角色id */
    private Long roleId;
    /** 创建人(用户id) */
    private Long createBy;
    /** 创建时间 */
    private Date createAt;
    /** 更新人(用户id) */
    private Long updateBy;
    /** 更新时间 */
    private Date updateAt;
}
