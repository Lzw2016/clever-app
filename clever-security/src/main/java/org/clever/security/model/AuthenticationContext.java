package org.clever.security.model;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/05 19:44 <br/>
 */
@Data
public class AuthenticationContext {
    /**
     * 请求对象
     */
    private final HttpServletRequest request;
    /**
     * 响应对象
     */
    private final HttpServletResponse response;
    /**
     * JWT-Token
     */
    private String jwtToken;
    /**
     * JWT刷新Token
     */
    private String refreshToken;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * JWT-Token body 信息
     */
    private Claims claims;
    /**
     * 安全上下文(用户信息)
     */
    private SecurityContext securityContext;

    public AuthenticationContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }
}
