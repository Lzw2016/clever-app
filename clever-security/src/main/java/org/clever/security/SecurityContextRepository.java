package org.clever.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.security.config.SecurityConfig;
import org.clever.security.model.LoginContext;
import org.clever.security.model.SecurityContext;

/**
 * 安全上下文(SecurityContext)存取器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 22:11 <br/>
 */
public interface SecurityContextRepository {
    /**
     * 缓存安全上下文(用户信息)
     *
     * @param context        用户登录上下文
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param response       响应对象
     */
    void cacheContext(LoginContext context, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response);

    /**
     * 加载安全上下文(用户信息)
     *
     * @param userId         用户id
     * @param claims         JWT-Token Body内容
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param response       响应对象
     */
    SecurityContext loadContext(Long userId, Claims claims, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response);
}
