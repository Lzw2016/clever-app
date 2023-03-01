package org.clever.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.core.mapper.JacksonMapper;
import org.clever.security.authentication.AuthenticationFilter;
import org.clever.security.authentication.token.RefreshJwtToken;
import org.clever.security.authentication.token.VerifyJwtToken;
import org.clever.security.authorization.AuthorizationFilter;
import org.clever.security.authorization.MvcAuthorizationVoter;
import org.clever.security.authorization.voter.AuthorizationVoter;
import org.clever.security.config.*;
import org.clever.security.crypto.AesPasswordEncoder;
import org.clever.security.crypto.BCryptPasswordEncoder;
import org.clever.security.crypto.PasswordEncoder;
import org.clever.security.crypto.RawPassword;
import org.clever.security.handler.*;
import org.clever.security.impl.DefaultSecurityContextRepository;
import org.clever.security.impl.authentication.DefaultRefreshJwtToken;
import org.clever.security.impl.authentication.DefaultVerifyJwtToken;
import org.clever.security.impl.handler.*;
import org.clever.security.impl.login.*;
import org.clever.security.login.*;
import org.clever.security.logout.LogoutFilter;
import org.clever.security.model.jackson2.SecurityJackson2Module;
import org.clever.security.utils.HttpRespondHandler;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/24 10:59 <br/>
 */
@Slf4j
public class SecurityBootstrap {
    /**
     * 密码加密实现<br />
     *
     * @see RawPassword RawPassword 不加密(明文)
     * @see AesPasswordEncoder AesPasswordEncoder 对称加密
     * @see BCryptPasswordEncoder BCryptPasswordEncoder 非对称加密
     */
    public static PasswordEncoder PASSWORD_ENCODER = new RawPassword();
    /**
     * 安全上下文(SecurityContext)存取器
     */
    public static SecurityContextRepository SECURITY_CONTEXT_REPOSITORY;
    /**
     * 使用JWT的刷新Token续期Token
     */
    public static RefreshJwtToken REFRESH_JWT_TOKEN;
    /**
     * 返回响应数据工具
     */
    public static HttpRespondHandler HTTP_RESPOND_HANDLER = HttpRespondHandler.INSTANCE;
    /**
     * 登录成功处理逻辑
     */
    public static final List<LoginSuccessHandler> LOGIN_SUCCESS_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 登录失败处理逻辑
     */
    public static final List<LoginFailureHandler> LOGIN_FAILURE_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 登出成功处理逻辑
     */
    public static final List<LogoutSuccessHandler> LOGOUT_SUCCESS_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 登出失败处理逻辑
     */
    public static final List<LogoutFailureHandler> LOGOUT_FAILURE_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 授权成功处理
     */
    public static final List<AuthorizationSuccessHandler> AUTHORIZATION_SUCCESS_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 授权失败处理
     */
    public static final List<AuthorizationFailureHandler> AUTHORIZATION_FAILURE_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 身份认证成功处理逻辑
     */
    public static final List<AuthenticationSuccessHandler> AUTHENTICATION_SUCCESS_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 身份认证失败处理逻辑
     */
    public static final List<AuthenticationFailureHandler> AUTHENTICATION_FAILURE_HANDLER_LIST = new ArrayList<>(1);
    /**
     * 授权投票器
     */
    public static final List<AuthorizationVoter> AUTHORIZATION_VOTER_LIST = new ArrayList<>(1);
    /**
     * JWT-Token验证器
     */
    public static final List<VerifyJwtToken> VERIFY_JWT_TOKEN_LIST = new ArrayList<>(1);
    /**
     * 收集登录信息
     */
    public static final List<LoginDataCollect> LOGIN_DATA_COLLECT_LIST = new ArrayList<>(1);
    /**
     * 验证用户登录请求数据
     */
    public static final List<VerifyLoginData> VERIFY_LOGIN_DATA_LIST = new ArrayList<>(1);
    /**
     * 加载用户信息(根据验证用户登录请求数据加载数据库中的用户信息)
     */
    public static final List<LoadUser> LOAD_USER_LIST = new ArrayList<>(1);
    /**
     * 验证用户信息(数据库中的用户信息)
     */
    public static final List<VerifyUserInfo> VERIFY_USER_INFO_LIST = new ArrayList<>(1);
    /**
     * 自定义扩展JWT信息
     */
    public static final List<AddJwtTokenExtData> ADD_JWT_TOKEN_EXT_DATA_LIST = new ArrayList<>(1);

    static {
        AUTHORIZATION_VOTER_LIST.add(new MvcAuthorizationVoter());
    }

    /**
     * 使用默认的“用户-角色-权限”逻辑实现
     */
    public static void useDefaultSecurity(SecurityConfig securityConfig) {
        SECURITY_CONTEXT_REPOSITORY = new DefaultSecurityContextRepository();
        // AuthenticationFilter
        REFRESH_JWT_TOKEN = new DefaultRefreshJwtToken(securityConfig);
        VERIFY_JWT_TOKEN_LIST.clear();
        VERIFY_JWT_TOKEN_LIST.add(new DefaultVerifyJwtToken());
        // LoginFilter
        LOGIN_DATA_COLLECT_LIST.clear();
        LOGIN_DATA_COLLECT_LIST.add(new DefaultLoginDataCollect());
        VERIFY_LOGIN_DATA_LIST.clear();
        VERIFY_LOGIN_DATA_LIST.add(new DefaultVerifyLoginData());
        LOAD_USER_LIST.clear();
        LOAD_USER_LIST.add(new DefaultLoadUser());
        VERIFY_USER_INFO_LIST.clear();
        VERIFY_USER_INFO_LIST.add(new DefaultVerifyUserInfo(PASSWORD_ENCODER));
        ADD_JWT_TOKEN_EXT_DATA_LIST.clear();
        ADD_JWT_TOKEN_EXT_DATA_LIST.add(new DefaultAddJwtTokenExtData());
        // AuthorizationFilter
        AUTHORIZATION_VOTER_LIST.clear();
        AUTHORIZATION_VOTER_LIST.add(new MvcAuthorizationVoter());
        // EventHandler
        LOGIN_SUCCESS_HANDLER_LIST.clear();
        LOGIN_SUCCESS_HANDLER_LIST.add(new DefaultLoginSuccessHandler(securityConfig));
        LOGIN_FAILURE_HANDLER_LIST.clear();
        LOGIN_FAILURE_HANDLER_LIST.add(new DefaultLoginFailureHandler());
        LOGOUT_SUCCESS_HANDLER_LIST.clear();
        LOGOUT_SUCCESS_HANDLER_LIST.add(new DefaultLogoutSuccessHandler(securityConfig));
        LOGOUT_FAILURE_HANDLER_LIST.clear();
        // LOGOUT_FAILURE_HANDLER_LIST.add();
        AUTHORIZATION_SUCCESS_HANDLER_LIST.clear();
        // AUTHORIZATION_SUCCESS_HANDLER_LIST.add();
        AUTHORIZATION_FAILURE_HANDLER_LIST.clear();
        // AUTHORIZATION_FAILURE_HANDLER_LIST.add();
        AUTHENTICATION_SUCCESS_HANDLER_LIST.clear();
        AUTHENTICATION_SUCCESS_HANDLER_LIST.add(new DefaultAuthenticationSuccessHandler());
        AUTHENTICATION_FAILURE_HANDLER_LIST.clear();
        AUTHENTICATION_FAILURE_HANDLER_LIST.add(new DefaultAuthenticationFailureHandler(securityConfig));
    }

    public static SecurityBootstrap create(SecurityConfig securityConfig) {
        return new SecurityBootstrap(securityConfig);
    }

    public static SecurityBootstrap create(Environment environment) {
        SecurityConfig securityConfig = Binder.get(environment).bind(SecurityConfig.PREFIX, SecurityConfig.class).orElseGet(SecurityConfig::new);
        AppContextHolder.registerBean("securityConfig", securityConfig, true);
        DataSourceConfig dataSource = Optional.of(securityConfig.getDataSource()).orElse(new DataSourceConfig());
        securityConfig.setDataSource(dataSource);
        LoginConfig login = Optional.of(securityConfig.getLogin()).orElse(new LoginConfig());
        securityConfig.setLogin(login);
        LogoutConfig logout = Optional.of(securityConfig.getLogout()).orElse(new LogoutConfig());
        securityConfig.setLogout(logout);
        AesKeyConfig reqAesKey = Optional.of(securityConfig.getReqAesKey()).orElse(new AesKeyConfig());
        securityConfig.setReqAesKey(reqAesKey);
        TokenConfig token = Optional.of(securityConfig.getToken()).orElse(new TokenConfig());
        securityConfig.setToken(token);
        List<String> logs = new ArrayList<>();
        logs.add("security: ");
        logs.add("  enable                : " + securityConfig.isEnable());
        logs.add("  dataSource: ");
        logs.add("    jdbcName            : " + dataSource.getJdbcName());
        logs.add("    enableRedis         : " + dataSource.isEnableRedis());
        logs.add("    redisName           : " + dataSource.getRedisName());
        logs.add("    redisNamespace      : " + dataSource.getRedisNamespace());
        logs.add("  ignorePaths           : " + StringUtils.join(securityConfig.getIgnorePaths(), " | "));
        logs.add("  ignoreAuthPaths       : " + StringUtils.join(securityConfig.getIgnoreAuthPaths(), " | "));
        logs.add("  ignoreAuthFailedPaths : " + StringUtils.join(securityConfig.getIgnoreAuthFailedPaths(), " | "));
        logs.add("  currentUserPath       : " + securityConfig.getCurrentUserPath());
        logs.add("  login: ");
        logs.add("    path                : " + StringUtils.join(login.getPaths(), " | "));
        logs.add("    postOnly            : " + login.isPostOnly());
        logs.add("    allowRepeatLogin    : " + login.isAllowRepeatLogin());
        logs.add("  logout.path           : " + logout.getPath());
        logs.add("  reqAesKey.enable      : " + reqAesKey.isEnable());
        logs.add("  token: ");
        logs.add("    useCookie           : " + token.isUseCookie());
        logs.add("    jwtTokenName        : " + token.getJwtTokenName());
        logs.add("    tokenValidity       : " + token.getTokenValidity().toDays() + "d");
        logs.add("    enableRefreshToken  : " + token.isEnableRefreshToken());
        logs.add("    refreshTokenName    : " + token.getRefreshTokenName());
        logs.add("    refreshTokenValidity: " + token.getRefreshTokenValidity().toDays() + "d");
        BannerUtils.printConfig(log, "security配置", logs.toArray(new String[0]));
        return create(securityConfig);
    }

    @Getter
    private final SecurityConfig securityConfig;
    /**
     * 身份认证拦截
     */
    private AuthenticationFilter authenticationFilter;
    /**
     * 登录拦截
     */
    private LoginFilter loginFilter;
    /**
     * 登出拦截
     */
    private LogoutFilter logoutFilter;
    /**
     * 权限授权拦截
     */
    private AuthorizationFilter authorizationFilter;

    public SecurityBootstrap(SecurityConfig securityConfig) {
        Assert.notNull(securityConfig, "参数 securityConfig 不能为 null");
        JacksonMapper.getInstance().getMapper().registerModule(SecurityJackson2Module.INSTANCE);
        // 配置数据源
        DataSourceConfig dataSource = securityConfig.getDataSource();
        if (dataSource != null) {
            SecurityDataSource.JDBC_DATA_SOURCE_NAME = dataSource.getJdbcName();
            SecurityDataSource.REDIS_DATA_SOURCE_NAME = dataSource.getRedisName();
            Assert.notNull(SecurityDataSource.getJdbc(), "配置 security.dataSource.jdbcName=“" + dataSource.getJdbcName() + "” 的Jdbc数据源不存在");
            if (dataSource.isEnableRedis()) {
                Assert.notNull(SecurityDataSource.getRedis(), "配置 security.dataSource.redisName=“" + dataSource.getRedisName() + "” 的Redis不存在");
            }
        }
        // 创建Filter对象
        this.securityConfig = securityConfig;
    }

    public synchronized AuthenticationFilter getAuthenticationFilter() {
        if (authenticationFilter == null) {
            authenticationFilter = new AuthenticationFilter(
                    securityConfig,
                    VERIFY_JWT_TOKEN_LIST,
                    SECURITY_CONTEXT_REPOSITORY,
                    AUTHENTICATION_SUCCESS_HANDLER_LIST,
                    AUTHENTICATION_FAILURE_HANDLER_LIST,
                    REFRESH_JWT_TOKEN,
                    HTTP_RESPOND_HANDLER
            );
        }
        return authenticationFilter;
    }

    public synchronized LoginFilter getLoginFilter() {
        if (loginFilter == null) {
            loginFilter = new LoginFilter(
                    securityConfig,
                    LOGIN_DATA_COLLECT_LIST,
                    VERIFY_LOGIN_DATA_LIST,
                    LOAD_USER_LIST,
                    VERIFY_USER_INFO_LIST,
                    ADD_JWT_TOKEN_EXT_DATA_LIST,
                    LOGIN_SUCCESS_HANDLER_LIST,
                    LOGIN_FAILURE_HANDLER_LIST,
                    SECURITY_CONTEXT_REPOSITORY,
                    HTTP_RESPOND_HANDLER
            );
        }
        return loginFilter;
    }

    public synchronized LogoutFilter getLogoutFilter() {
        if (logoutFilter == null) {
            logoutFilter = new LogoutFilter(
                    securityConfig,
                    LOGOUT_SUCCESS_HANDLER_LIST,
                    LOGOUT_FAILURE_HANDLER_LIST,
                    HTTP_RESPOND_HANDLER
            );
        }
        return logoutFilter;
    }

    public synchronized AuthorizationFilter getAuthorizationFilter() {
        if (authorizationFilter == null) {
            authorizationFilter = new AuthorizationFilter(
                    securityConfig,
                    AUTHORIZATION_VOTER_LIST,
                    AUTHORIZATION_SUCCESS_HANDLER_LIST,
                    AUTHORIZATION_FAILURE_HANDLER_LIST,
                    HTTP_RESPOND_HANDLER
            );
        }
        return authorizationFilter;
    }
}
