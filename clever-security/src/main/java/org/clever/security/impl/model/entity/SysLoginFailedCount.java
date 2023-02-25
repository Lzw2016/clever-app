package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连续登录失败次数(缓存表)(sys_login_failed_count)
 */
@Data
public class SysLoginFailedCount implements Serializable {
    /** 主键id */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 登录方式 */
    private Integer loginType;
    /** 登录失败次数 */
    private Integer failedCount;
    /** 最后登录失败时间 */
    private Date lastLoginTime;
    /** 数据删除标志: 0:未删除，1:已删除 */
    private Integer deleteFlag;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
