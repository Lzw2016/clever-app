package org.clever.security.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2020-12-24 21:28 <br/>
 */
@Data
public class UseJwtRefreshToken implements Serializable {
    /**
     * 过期的JWT-Token id
     */
    private Long useJwtId;
    /**
     * 使用的刷新Token
     */
    private String useRefreshToken;

    /**
     * 新的JWT-Token id
     */
    private Long jwtId;
    /**
     * 新的token数据
     */
    private String token;
    /**
     * 新的JWT-Token过期时间(空表示永不过期)
     */
    private Date expiredTime;
    /**
     * 新的刷新Token
     */
    private String refreshToken;
    /**
     * 新的刷新Token过期时间
     */
    private Date refreshTokenExpiredTime;
}
