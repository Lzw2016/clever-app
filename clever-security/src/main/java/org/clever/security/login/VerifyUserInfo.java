package org.clever.security.login;

import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.LoginException;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractLoginReq;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/01 21:05 <br/>
 */
public interface VerifyUserInfo extends Ordered {
    /**
     * 是否支持验证登录用户信息
     *
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param loginReq       登录请求参数
     * @param userInfo       用户信息(数据库中的用户数据)
     * @return 返回true表示支持搜集
     */
    boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq, UserInfo userInfo);

    /**
     * 用户登录验证
     *
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param loginReq       登录请求参数
     * @param userInfo       用户信息(数据库中的用户数据)
     * @throws LoginException 验证失败
     */
    void verify(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq, UserInfo userInfo) throws LoginException;

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
