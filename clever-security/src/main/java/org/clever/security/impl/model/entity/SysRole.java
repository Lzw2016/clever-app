package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色表(sys_role)
 */
@Data
public class SysRole implements Serializable {
    /** 角色id */
    private Long id;
    /** 角色编号 */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 是否启用: 0:禁用，1:启用 */
    private Integer isEnable;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private Date createAt;
    /** 更新人 */
    private String updateBy;
    /** 更新时间 */
    private Date updateAt;
}
