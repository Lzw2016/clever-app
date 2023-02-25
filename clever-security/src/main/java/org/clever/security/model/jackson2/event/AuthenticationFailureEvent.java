package org.clever.security.model.jackson2.event;

import io.jsonwebtoken.Claims;
import lombok.Data;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/05 18:44 <br/>
 */
@Data
public class AuthenticationFailureEvent implements Serializable {
    /**
     * JWT-Token (可能为空)
     */
    private final String jwtToken;
    /**
     * 用户id (可能为空)
     */
    private final Long userId;
    /**
     * JWT-Token body 信息 (可能为空)
     */
    private final Claims claims;

    public AuthenticationFailureEvent(String jwtToken, Long userId, Claims claims) {
        this.jwtToken = jwtToken;
        this.userId = userId;
        this.claims = claims;
    }
}
