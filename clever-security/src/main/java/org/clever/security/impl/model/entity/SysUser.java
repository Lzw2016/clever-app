package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表(sys_user)
 */
@Data
public class SysUser implements Serializable {
    /** 用户id */
    private Long id;
    /** 用户编号 */
    private String userCode;
    /** 登录名 */
    private String userName;
    /** 是否启用;0:禁用，1:启用 */
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
