package org.clever.security.authorization.voter;

import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.SecurityConfig;
import org.clever.security.model.SecurityContext;

import javax.servlet.http.HttpServletRequest;

/**
 * 授权投票器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 18:08 <br/>
 */
public interface AuthorizationVoter extends Ordered {
    long DEFAULT_WEIGHT = 1L;

    /**
     * 返回当前投票器权重
     */
    default long getWeight() {
        return DEFAULT_WEIGHT;
    }

    /**
     * 是否支持收集当前用户登录信息
     *
     * @param securityConfig  权限系统配置
     * @param request         请求对象
     * @param securityContext 安全上下文(用户信息)
     * @return 返回true表示支持对当前请求授权投票
     */
    default boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext) {
        return true;
    }

    /**
     * 投票当前用户是否能访问资源
     *
     * @param securityConfig  权限系统配置
     * @param request         请求对象
     * @param securityContext 安全上下文(用户信息)
     * @return 投票结果
     */
    VoterResult vote(SecurityConfig securityConfig, HttpServletRequest request, SecurityContext securityContext);

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
