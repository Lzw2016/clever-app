package org.clever.security.login;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.DateUtils;
import org.clever.core.OrderComparator;
import org.clever.core.http.CookieUtils;
import org.clever.core.tuples.TupleTwo;
import org.clever.security.SecurityContextHolder;
import org.clever.security.SecurityContextRepository;
import org.clever.security.config.LoginConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.config.TokenConfig;
import org.clever.security.exception.CollectLoginDataException;
import org.clever.security.exception.LoginException;
import org.clever.security.exception.LoginInnerException;
import org.clever.security.exception.RepeatLoginException;
import org.clever.security.handler.LoginFailureHandler;
import org.clever.security.handler.LoginSuccessHandler;
import org.clever.security.model.LoginContext;
import org.clever.security.model.UserInfo;
import org.clever.security.model.jackson2.event.LoginFailureEvent;
import org.clever.security.model.jackson2.event.LoginSuccessEvent;
import org.clever.security.model.request.AbstractUserLoginReq;
import org.clever.security.model.response.LoginRes;
import org.clever.security.utils.HttpServletResponseUtils;
import org.clever.security.utils.JwtTokenUtils;
import org.clever.security.utils.PathFilterUtils;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.http.HttpStatus;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 登录拦截器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:09 <br/>
 */
@Slf4j
public class LoginFilter implements FilterRegistrar.FilterFuc {
    /**
     * 全局配置
     */
    private final SecurityConfig securityConfig;
    /**
     * 收集登录数据
     */
    private final List<LoginDataCollect> loginDataCollectList;
    /**
     * 加载用户之前校验登录数据(字段格式、验证码等等)
     */
    private final List<VerifyLoginData> verifyLoginDataList;
    /**
     * 加载用户信息
     */
    private final List<LoadUser> loadUserList;
    /**
     * 加载用户之后校验登录数据(密码、验证码等)
     */
    private final List<VerifyUserInfo> verifyUserInfoList;
    /**
     * 创建JWT-Token时加入扩展数据
     */
    private final List<AddJwtTokenExtData> addJwtTokenExtDataList;
    /**
     * 登录成功处理
     */
    private final List<LoginSuccessHandler> loginSuccessHandlerList;
    /**
     * 登录失败处理
     */
    private final List<LoginFailureHandler> loginFailureHandlerList;
    /**
     * 加载安全上下文(用户信息)
     */
    private final SecurityContextRepository securityContextRepository;

    public LoginFilter(
            SecurityConfig securityConfig,
            List<LoginDataCollect> loginDataCollectList,
            List<VerifyLoginData> verifyLoginDataList,
            List<LoadUser> loadUserList,
            List<VerifyUserInfo> verifyUserInfoList,
            List<AddJwtTokenExtData> addJwtTokenExtDataList,
            List<LoginSuccessHandler> loginSuccessHandlerList,
            List<LoginFailureHandler> loginFailureHandlerList,
            SecurityContextRepository securityContextRepository) {
        Assert.notNull(securityConfig, "权限系统配置对象(SecurityConfig)不能为null");
        Assert.notEmpty(loginDataCollectList, "登录数据收集器(LoginDataCollect)不存在");
        Assert.notEmpty(verifyLoginDataList, "用户登录验证器(VerifyLoginData)不存在");
        Assert.notEmpty(loadUserList, "用户信息加载器(LoadUser)不存在");
        Assert.notEmpty(verifyUserInfoList, "用户登录验证器(VerifyUserInfo)不存在");
        Assert.notNull(securityContextRepository, "安全上下文存取器(SecurityContextRepository)不能为null");
        OrderComparator.sort(loginDataCollectList);
        OrderComparator.sort(verifyLoginDataList);
        OrderComparator.sort(loadUserList);
        OrderComparator.sort(verifyUserInfoList);
        OrderComparator.sort(addJwtTokenExtDataList);
        OrderComparator.sort(loginSuccessHandlerList);
        OrderComparator.sort(loginFailureHandlerList);
        this.securityConfig = securityConfig;
        this.loginDataCollectList = loginDataCollectList;
        this.verifyLoginDataList = verifyLoginDataList;
        this.loadUserList = loadUserList;
        this.verifyUserInfoList = verifyUserInfoList;
        this.addJwtTokenExtDataList = addJwtTokenExtDataList;
        this.loginSuccessHandlerList = loginSuccessHandlerList;
        this.loginFailureHandlerList = loginFailureHandlerList;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        // 不是登录请求
        if (!PathFilterUtils.isLoginRequest(ctx.req, securityConfig)) {
            ctx.next();
            return;
        }
        log.debug("### 开始执行登录逻辑 ---------------------------------------------------------------------->");
        log.debug("当前请求 -> [{}]", ctx.req.getRequestURI());
        // 执行登录逻辑
        LoginContext context = new LoginContext(ctx.req, ctx.res);
        try {
            // 执行登录流程
            login(context);
            // 登录成功处理
            loginSuccessHandler(context);
            // 登录成功 - 返回数据给客户端
            onLoginSuccessResponse(context);
        } catch (LoginException e) {
            // 登录失败
            log.debug("### 登录失败", e);
            if (context.getLoginException() == null) {
                context.setLoginException(e);
            }
            try {
                // 登录失败处理
                loginFailureHandler(context);
                // 返回数据给客户端
                onLoginFailureResponse(context);
            } catch (Exception innerException) {
                log.error("登录异常", innerException);
                HttpServletResponseUtils.sendJson(ctx.req, ctx.res, HttpStatus.INTERNAL_SERVER_ERROR, innerException);
            }
        } catch (Throwable e) {
            // 登录异常
            log.error("登录异常", e);
            HttpServletResponseUtils.sendJson(ctx.req, ctx.res, HttpStatus.INTERNAL_SERVER_ERROR, e);
        } finally {
            log.debug("### 登录逻辑执行完成 <----------------------------------------------------------------------");
        }
    }

    /**
     * 登录流程
     */
    protected void login(LoginContext context) throws Exception {
        // 判断用户是否重复登录
        if (!securityConfig.getLogin().isAllowRepeatLogin() && SecurityContextHolder.containsContext(context.getRequest())) {
            context.setLoginException(new RepeatLoginException("不支持用户重复登录,必须先退出当前账户"));
            throw context.getLoginException();
        }
        // 收集登录数据
        AbstractUserLoginReq loginReq = null;
        for (LoginDataCollect collect : loginDataCollectList) {
            if (!collect.isSupported(securityConfig, PathFilterUtils.getPath(context.getRequest()), context.getRequest())) {
                continue;
            }
            try {
                loginReq = collect.collectLoginData(securityConfig, context.getRequest());
            } catch (Exception e) {
                context.setLoginException(new CollectLoginDataException("读取登录数据失败", e));
                throw context.getLoginException();
            }
            if (loginReq != null) {
                break;
            }
        }
        log.debug("### 收集登录数据 -> {}", loginReq);
        if (loginReq == null) {
            context.setLoginException(new LoginInnerException("不支持的登录请求(无法获取登录数据)"));
            throw context.getLoginException();
        }
        context.setLoginData(loginReq);
        // 加载用户之前校验登录数据
        for (VerifyLoginData verifyLoginData : verifyLoginDataList) {
            if (!verifyLoginData.isSupported(securityConfig, context.getRequest(), loginReq)) {
                continue;
            }
            try {
                verifyLoginData.verify(securityConfig, context.getRequest(), loginReq);
            } catch (LoginException e) {
                context.setLoginException(e);
                break;
            }
        }
        // 登录失败
        if (context.isLoginFailure()) {
            log.debug("### 校验登录数据失败(登录失败) -> {}", loginReq);
            throw context.getLoginException();
        }
        log.debug("### 校验登录数据成功 -> {}", loginReq);
        // 加载用户信息
        LoadUser loadUser = null;
        for (LoadUser load : loadUserList) {
            if (load.isSupported(securityConfig, context.getRequest(), loginReq)) {
                loadUser = load;
                break;
            }
        }
        if (loadUser == null) {
            context.setLoginException(new LoginInnerException("用户信息不存在(无法加载用户信息)"));
            throw context.getLoginException();
        }
        UserInfo userInfo = loadUser.loadUserInfo(securityConfig, context.getRequest(), loginReq);
        context.setUserInfo(userInfo);
        log.debug("### 加载用户信息 -> {}", userInfo);
        // 加载用户之后校验用户信息
        for (VerifyUserInfo verifyUserInfo : verifyUserInfoList) {
            if (!verifyUserInfo.isSupported(securityConfig, context.getRequest(), loginReq, userInfo)) {
                continue;
            }
            try {
                verifyUserInfo.verify(securityConfig, context.getRequest(), loginReq, userInfo);
            } catch (LoginException e) {
                context.setLoginException(e);
                break;
            }
        }
        // 登录失败
        if (context.isLoginFailure()) {
            log.debug("### 校验登录数据失败(登录失败) -> {}", userInfo);
            throw context.getLoginException();
        }
        // 登录成功
        log.debug("### 登录成功 -> {}", userInfo);
        final Date now = new Date();
        TokenConfig tokenConfig = securityConfig.getToken();
        final TupleTwo<String, Claims> tokenInfo = JwtTokenUtils.createJwtToken(context.getRequest(), tokenConfig, userInfo, addJwtTokenExtDataList);
        String refreshToken = null;
        if (tokenConfig.isEnableRefreshToken()) {
            refreshToken = JwtTokenUtils.createRefreshToken(userInfo.getUserId());
            context.setRefreshToken(refreshToken);
            context.setRefreshTokenExpiredTime(new Date(now.getTime() + tokenConfig.getRefreshTokenValidity().toMillis()));
        }
        log.debug("### 登录成功 | userId={} | jwt-token={} | refresh-token={}", userInfo.strUserId(), tokenInfo.getValue1(), refreshToken);
        context.setJwtToken(tokenInfo.getValue1());
        context.setClaims(tokenInfo.getValue2());
        // 保存安全上下文(用户信息)
        securityContextRepository.cacheContext(context, securityConfig, context.getRequest(), context.getResponse());
        // 将JWT-Token写入客户端
        if (tokenConfig.isUseCookie()) {
            int tokenMaxAge = DateUtils.pastSeconds(now, tokenInfo.getValue2().getExpiration());
            int refreshTokenMaxAge = context.getRefreshTokenExpiredTime() == null ? 0 : DateUtils.pastSeconds(now, context.getRefreshTokenExpiredTime());
            int maxAge = Integer.max(refreshTokenMaxAge, tokenMaxAge) + (60 * 3);
            CookieUtils.setCookie(context.getResponse(), "/", tokenConfig.getJwtTokenName(), tokenInfo.getValue1(), maxAge);
            if (tokenConfig.isEnableRefreshToken() && refreshToken != null) {
                CookieUtils.setCookie(context.getResponse(), "/", tokenConfig.getRefreshTokenName(), refreshToken, maxAge);
            }
        } else {
            context.getResponse().addHeader(tokenConfig.getJwtTokenName(), tokenInfo.getValue1());
            if (tokenConfig.isEnableRefreshToken()) {
                context.getResponse().addHeader(tokenConfig.getRefreshTokenName(), refreshToken);
            }
        }
    }

    /**
     * 登录成功处理
     */
    protected void loginSuccessHandler(LoginContext context) throws Exception {
        if (loginSuccessHandlerList == null) {
            return;
        }
        LoginSuccessEvent loginSuccessEvent = new LoginSuccessEvent(
                context.getRequest(),
                context.getResponse(),
                securityConfig.getLogin(),
                context.getLoginData(),
                context.getUserInfo(),
                context.getJwtToken(),
                context.getClaims()
        );
        if (StringUtils.isNotBlank(context.getRefreshToken())) {
            loginSuccessEvent.setRefreshToken(context.getRefreshToken());
            loginSuccessEvent.setRtExpiredTime(context.getRefreshTokenExpiredTime());
        }
        for (LoginSuccessHandler handler : loginSuccessHandlerList) {
            handler.onLoginSuccess(context.getRequest(), context.getResponse(), loginSuccessEvent);
        }
    }

    /**
     * 登录失败处理
     */
    protected void loginFailureHandler(LoginContext context) throws Exception {
        if (loginFailureHandlerList == null) {
            return;
        }
        LoginFailureEvent loginFailureEvent = new LoginFailureEvent(
                context.getRequest(),
                context.getResponse(),
                context.getLoginData(),
                context.getUserInfo(),
                context.getLoginException()
        );
        for (LoginFailureHandler handler : loginFailureHandlerList) {
            handler.onLoginFailure(context.getRequest(), context.getResponse(), loginFailureEvent);
        }
    }

    /**
     * 当登录成功时响应处理
     */
    protected void onLoginSuccessResponse(LoginContext context) throws IOException {
        if (context.getResponse().isCommitted()) {
            return;
        }
        LoginConfig login = securityConfig.getLogin();
        if (login != null && login.isSuccessNeedRedirect()) {
            // 需要重定向
            HttpServletResponseUtils.redirect(context.getResponse(), login.getSuccessRedirectPage());
        } else {
            // 直接返回
            UserInfo userInfo = context.getUserInfo().copy();
            LoginRes loginRes = LoginRes.loginSuccess(userInfo, context.getJwtToken(), context.getRefreshToken());
            HttpServletResponseUtils.sendJson(context.getResponse(), loginRes, HttpStatus.OK);
        }
    }

    /**
     * 当登录失败时响应处理
     */
    protected void onLoginFailureResponse(LoginContext context) throws IOException {
        if (context.getResponse().isCommitted()) {
            return;
        }
        LoginConfig login = securityConfig.getLogin();
        if (login.isFailureNeedRedirect()) {
            // 需要重定向
            HttpServletResponseUtils.redirect(context.getResponse(), login.getFailureRedirectPage());
        } else {
            // 直接返回
            LoginRes loginRes = LoginRes.loginFailure(context.getLoginException().getMessage());
            HttpStatus httpStatus = (context.getLoginException() instanceof RepeatLoginException) ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
            HttpServletResponseUtils.sendJson(context.getResponse(), loginRes, httpStatus);
        }
    }
}
