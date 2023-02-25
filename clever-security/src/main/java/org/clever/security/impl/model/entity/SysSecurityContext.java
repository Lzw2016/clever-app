package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户security context(缓存表)(sys_security_context)
 */
@Data
public class SysSecurityContext implements Serializable {
    /** 主键id */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 用户security context */
    private String securityContext;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
