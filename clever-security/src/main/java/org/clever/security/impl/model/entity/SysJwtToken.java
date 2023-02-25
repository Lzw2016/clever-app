package org.clever.security.impl.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * jwt-token表(缓存表)(sys_jwt_token)
 */
@Data
public class SysJwtToken implements Serializable {
    /** token id */
    private Long id;
    /** 用户id */
    private Long userId;
    /** token数据 */
    private String token;
    /** token过期时间(空表示永不过期) */
    private Date expiredTime;
    /** token是否禁用;0:未禁用；1:已禁用 */
    private Integer disable;
    /** token禁用原因: 0:使用RefreshToken；1:管理员手动禁用；2:并发登录被挤下线；3:用户主动登出 */
    private Integer disableReason;
    /** 刷新token */
    private String refreshToken;
    /** 刷新token过期时间 */
    private Date rtExpiredTime;
    /** 刷新token状态;0:无效(已使用)；1:有效(未使用) */
    private Integer rtState;
    /** 刷新token使用时间 */
    private Date rtUseTime;
    /** 刷新token创建的token id */
    private Long rtCreateTokenId;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
