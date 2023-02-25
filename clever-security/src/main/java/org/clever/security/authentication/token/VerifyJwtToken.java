package org.clever.security.authentication.token;

import io.jsonwebtoken.Claims;
import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT-Token验证器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/12/05 12:40 <br/>
 */
public interface VerifyJwtToken extends Ordered {
    /**
     * 验证JWT-Token
     *
     * @param jwtToken       JWT-Token
     * @param userId         用户id
     * @param claims         JWT-Token Body内容
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param response       响应对象
     */
    void verify(String jwtToken, Long userId, Claims claims, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException;

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
