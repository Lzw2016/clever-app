package org.clever.security.login;

import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.SecurityConfig;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractLoginReq;

import javax.servlet.http.HttpServletRequest;

/**
 * 加载用户信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 19:23 <br/>
 */
public interface LoadUser extends Ordered {
    /**
     * 是否支持加载用户信息
     *
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param loginReq       登录请求参数
     * @return 返回true表示支持搜集
     */
    boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq);

    /**
     * 加载用户信息(从数据库或其他系统中加载)
     *
     * @param securityConfig 权限系统配置
     * @param request        请求对象
     * @param loginReq       登录请求参数
     * @return 用户信息(不存在返回null)
     */
    UserInfo loadUserInfo(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq);

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
