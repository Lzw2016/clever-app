package org.clever.security.login;

import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.OrderIncrement;
import org.clever.core.Ordered;
import org.clever.security.config.SecurityConfig;
import org.clever.security.model.request.AbstractLoginReq;

/**
 * 收集登录信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 14:14 <br/>
 */
public interface LoginDataCollect extends Ordered {
    /**
     * 是否支持收集当前用户登录信息
     *
     * @param securityConfig 权限系统配置
     * @param loginPath      登录路径(配置{@code security.login.paths}中的一项)
     * @param request        请求对象
     * @return 返回true表示支持搜集
     */
    boolean isSupported(SecurityConfig securityConfig, String loginPath, HttpServletRequest request);

    /***
     * 收集登录请求数据
     * @param securityConfig 权限系统配置
     * @param request           请求对象
     * @return 登录数据对象
     */
    AbstractLoginReq collectLoginData(SecurityConfig securityConfig, HttpServletRequest request);

    @Override
    default double getOrder() {
        return OrderIncrement.NORMAL;
    }
}
