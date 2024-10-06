package org.clever.security.model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.clever.security.exception.AuthorizationException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 12:33 <br/>
 */
@Data
public class AuthorizationContext {
    /**
     * 请求对象
     */
    private final HttpServletRequest request;
    /**
     * 响应对象
     */
    private final HttpServletResponse response;
    /**
     * 安全上下文(用户信息)
     */
    private SecurityContext securityContext;
    /**
     * 授权异常信息
     */
    private AuthorizationException authorizationException;

    public AuthorizationContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }
}
