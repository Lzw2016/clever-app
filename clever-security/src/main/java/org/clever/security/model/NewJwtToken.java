package org.clever.security.model;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/30 19:35 <br/>
 */
@Data
public class NewJwtToken implements Serializable {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * token数据
     */
    private String token;
    /**
     * jwt-token过期时间(空表示永不过期)
     */
    private Timestamp expiredTime;
    /**
     * 刷新token
     */
    private String refreshToken;
    /**
     * 刷新token过期时间
     */
    private Timestamp rtExpiredTime;
    /**
     * 刷新token创建的jwt-token id
     */
    private Long rtCreateTokenId;
}
