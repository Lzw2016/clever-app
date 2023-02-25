package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志(sys_login_log)
 */
@Data
public class SysLoginLog implements Serializable {
    /** 主键id */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 登录时间 */
    private Date loginTime;
    /** 登录ip */
    private String loginIp;
    /** 登录方式 */
    private Integer loginType;
    /** 登录渠道 */
    private Integer loginChannel;
    /** 登录状态: 0:登录失败，1:登录成功 */
    private Integer loginState;
    /** 登录请求数据 */
    private String requestData;
    /** jwt;token id */
    private Long jwtTokenId;
    /** 创建时间 */
    private Date createAt;
}
