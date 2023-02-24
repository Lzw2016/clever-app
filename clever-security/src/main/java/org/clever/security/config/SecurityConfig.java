package org.clever.security.config;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/15 15:14 <br/>
 */
@Data
public class SecurityConfig implements Serializable {
    public static final String PREFIX = "web.security";

    /**
     * 是有启用 security
     */
    private boolean enable = true;
    /**
     * security 使用的数据源配置
     */
    private DataSourceConfig dataSource = new DataSourceConfig();
    /**
     * 不需要认证和授权的Path(支持Ant风格的Path)
     */
    private List<String> ignorePaths = new ArrayList<>();
    /**
     * 不需要授权的Path(支持Ant风格的Path)
     */
    private List<String> ignoreAuthPaths = new ArrayList<>();
    /**
     * 认证或授权失败时不拦截的Path(支持Ant风格的Path)
     */
    private List<String> ignoreAuthFailedPaths = new ArrayList<>();
    /**
     * 未登录时是否需要重定向到 401 页面
     */
    private boolean notLoginNeedRedirect = false;
    /**
     * 未登录时是否需要重定向
     */
    private String notLoginRedirectPage = "/index.html";
    /**
     * 无权访问时是否需要重定向到 403 页面
     */
    private boolean forbiddenNeedRedirect = false;
    /**
     * 403页面地址(无权访问时的重定向地址)
     */
    private String forbiddenRedirectPage = "/403.html";
    /**
     * 获取当前用户信息请求地址
     */
    private String currentUserPath = "/current_user";
    /**
     * 用户登录相关配置
     */
    private LoginConfig login = new LoginConfig();
    /**
     * 用户登出相关配置
     */
    private LogoutConfig logout = new LogoutConfig();
    /**
     * 用户请求参数加密配置AesKey(登录、注册等敏感接口使用)
     */
    private AesKeyConfig reqAesKey = new AesKeyConfig();
    /**
     * token配置(JWT-Token有效)
     */
    private TokenConfig token = new TokenConfig();
}
