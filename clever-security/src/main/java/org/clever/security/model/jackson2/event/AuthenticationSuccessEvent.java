package org.clever.security.model.jackson2.event;

import io.jsonwebtoken.Claims;
import lombok.Data;
import org.clever.security.model.SecurityContext;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/05 18:45 <br/>
 */
@Data
public class AuthenticationSuccessEvent implements Serializable {
    /**
     * JWT-Token
     */
    private final String jwtToken;
    /**
     * 用户id
     */
    private final String userId;
    /**
     * JWT-Token body 信息
     */
    private final Claims claims;
    /**
     * 安全上下文(用户信息)
     */
    private final SecurityContext securityContext;

    public AuthenticationSuccessEvent(String jwtToken, String userId, Claims claims, SecurityContext securityContext) {
        this.jwtToken = jwtToken;
        this.userId = userId;
        this.claims = claims;
        this.securityContext = securityContext;
    }
}
