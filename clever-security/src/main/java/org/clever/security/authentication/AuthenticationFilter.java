//package org.clever.security.authentication;
//
//import org.clever.core.DateUtils;
//import org.clever.core.http.CookieUtils;
//import org.clever.security.JwtTokenHolder;
//import org.clever.security.SecurityContextHolder;
//import org.clever.security.SecurityContextRepository;
//import org.clever.security.authentication.token.RefreshJwtToken;
//import org.clever.security.authentication.token.VerifyJwtToken;
//import org.clever.security.config.SecurityConfig;
//import org.clever.security.config.TokenConfig;
//import org.clever.security.exception.AuthenticationException;
//import org.clever.security.exception.InvalidJwtRefreshTokenException;
//import org.clever.security.exception.ParserJwtTokenException;
//import org.clever.security.handler.AuthenticationFailureHandler;
//import org.clever.security.handler.AuthenticationSuccessHandler;
//import org.clever.security.model.AuthenticationContext;
//import org.clever.security.model.NewJwtToken;
//import org.clever.security.model.SecurityContext;
//import org.clever.security.model.UseJwtRefreshToken;
//import org.clever.security.model.event.AuthenticationFailureEvent;
//import org.clever.security.model.event.AuthenticationSuccessEvent;
//import org.clever.security.utils.HttpServletResponseUtils;
//import org.clever.security.utils.JwtTokenUtils;
//import org.clever.security.utils.ListSortUtils;
//import org.clever.security.utils.PathFilterUtils;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpFilter;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.Date;
//import java.util.List;
//
///**
// * 用户身份认证拦截器
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2020/11/29 21:27 <br/>
// */
//@Slf4j
//public class AuthenticationFilter extends HttpFilter {
//    public final static String JWT_OBJECT_REQUEST_ATTRIBUTE = AuthenticationFilter.class.getName() + "_JWT_Object";
//    /**
//     * 全局配置
//     */
//    private final SecurityConfig securityConfig;
//    /**
//     * JWT-Token验证器
//     */
//    private final List<VerifyJwtToken> verifyJwtTokenList;
//    /**
//     * 加载安全上下文(用户信息)
//     */
//    private final SecurityContextRepository securityContextRepository;
//    /**
//     * 用户身份认成功处理
//     */
//    private final List<AuthenticationSuccessHandler> authenticationSuccessHandlerList;
//    /**
//     * 用户身份认失败处理
//     */
//    private final List<AuthenticationFailureHandler> authenticationFailureHandlerList;
//    /**
//     * 登录实现对象
//     */
//    private final RefreshJwtToken refreshJwtToken;
//
//    public AuthenticationFilter(SecurityConfig securityConfig,
//                                List<VerifyJwtToken> verifyJwtTokenList,
//                                SecurityContextRepository securityContextRepository,
//                                List<AuthenticationSuccessHandler> authenticationSuccessHandlerList,
//                                List<AuthenticationFailureHandler> authenticationFailureHandlerList,
//                                RefreshJwtToken refreshJwtToken) {
//        Assert.notNull(securityConfig, "权限系统配置对象(SecurityConfig)不能为null");
//        Assert.notEmpty(verifyJwtTokenList, "JWT-Token验证器(VerifyJwtToken)不存在");
//        Assert.notNull(securityContextRepository, "安全上下文存取器(SecurityContextRepository)不能为null");
//        Assert.notNull(authenticationSuccessHandlerList, "参数authenticationSuccessHandlerList不能为null");
//        Assert.notNull(authenticationFailureHandlerList, "参数authenticationFailureHandlerList不能为null");
//        Assert.notNull(refreshJwtToken, "参数refreshJwtToken不能为null");
//        this.securityConfig = securityConfig;
//        this.verifyJwtTokenList = ListSortUtils.sort(verifyJwtTokenList);
//        this.securityContextRepository = securityContextRepository;
//        this.authenticationSuccessHandlerList = ListSortUtils.sort(authenticationSuccessHandlerList);
//        this.authenticationFailureHandlerList = ListSortUtils.sort(authenticationFailureHandlerList);
//        this.refreshJwtToken = refreshJwtToken;
//    }
//
//    @Override
//    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
//        // 是否是跨域请求
//        if (PathFilterUtils.isPreFlightRequest(request, response)) {
//            return;
//        }
//        // 是否是登出请求
//        if (PathFilterUtils.isLogoutRequest(request, securityConfig)) {
//            doAuthentication(request, response, false);
//            innerDoFilter(request, response, chain);
//            return;
//        }
//        // 是否不需要身份认证
//        if (!PathFilterUtils.isAuthenticationRequest(request, securityConfig)) {
//            if (PathFilterUtils.isLoginRequest(request, securityConfig) && !securityConfig.getLogin().isAllowRepeatLogin()) {
//                // 当前请求是登录请求且不允许重复登录时，需要判断当前用户是否已经登录
//                doAuthentication(request, response, false);
//            }
//            innerDoFilter(request, response, chain);
//            return;
//        }
//        // 需要认证
//        if (doAuthentication(request, response, true)) {
//            innerDoFilter(request, response, chain);
//        }
//    }
//
//    protected void innerDoFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
//        try {
//            if (PathFilterUtils.isGetCurrentUserRequest(request, securityConfig)) {
//                // 获取当前登录用户信息
//                SecurityContext securityContext = SecurityContextHolder.getContext();
//                SecurityContext data = securityContext.copy();
//                HttpServletResponseUtils.sendJson(response, data);
//            } else {
//                // 处理业务逻辑
//                chain.doFilter(request, response);
//            }
//        } finally {
//            try {
//                // 清理数据防止内存泄漏
//                SecurityContextHolder.clearContext();
//                JwtTokenHolder.clear();
//            } catch (Exception e) {
//                log.warn("clearSecurityContext失败", e);
//            }
//        }
//    }
//
//    /**
//     * 执行用户身份认证
//     *
//     * @param request      请求对象
//     * @param response     响应对象
//     * @param sendResponse 是否需要返回数据给客户端
//     * @return true:认证成功, false:认证失败
//     */
//    protected boolean doAuthentication(HttpServletRequest request, HttpServletResponse response, boolean sendResponse) throws IOException {
//        log.debug("### 开始执行认证逻辑 ---------------------------------------------------------------------->");
//        log.debug("当前请求 -> [{}]", request.getRequestURI());
//        // 执行认证逻辑
//        AuthenticationContext context = new AuthenticationContext(request, response);
//        try {
//            // 执行认证流程
//            authentication(context);
//            // 用户身份认成功处理
//            authenticationSuccessHandler(context);
//        } catch (AuthenticationException e) {
//            // 认证或授权失败时不拦截的请求
//            if (PathFilterUtils.isIgnoreAuthFailedRequest(request, securityConfig)) {
//                return true;
//            }
//            // 认证失败
//            log.debug("### 认证失败", e);
//            try {
//                // 用户身份认失败处理
//                authenticationFailureHandler(context);
//                // 返回数据给客户端
//                if (sendResponse) {
//                    onAuthenticationFailureResponse(request, response, e);
//                }
//            } catch (Exception innerException) {
//                log.error("认证异常", innerException);
//                // 返回数据给客户端
//                if (sendResponse) {
//                    HttpServletResponseUtils.sendJson(request, response, HttpStatus.INTERNAL_SERVER_ERROR, innerException);
//                }
//            }
//            return false;
//        } catch (Exception e) {
//            // 认证异常 - 返回数据给客户端
//            log.error("认证异常", e);
//            if (sendResponse) {
//                HttpServletResponseUtils.sendJson(request, response, HttpStatus.INTERNAL_SERVER_ERROR, e);
//            }
//            return false;
//        } finally {
//            log.debug("### 认证逻辑执行完成 <----------------------------------------------------------------------");
//        }
//        return true;
//    }
//
//    /**
//     * 认证流程
//     */
//    protected void authentication(AuthenticationContext context) {
//        final Date now = new Date();
//        // 用户登录身份认证
//        TokenConfig tokenConfig = securityConfig.getToken();
//        // 获取JWT-Token
//        String jwtToken;
//        String refreshToken;
//        jwtToken = CookieUtils.getCookie(context.getRequest(), tokenConfig.getJwtTokenName());
//        if (StringUtils.isBlank(jwtToken)) {
//            jwtToken = context.getRequest().getHeader(tokenConfig.getJwtTokenName());
//        }
//        refreshToken = CookieUtils.getCookie(context.getRequest(), tokenConfig.getRefreshTokenName());
//        if (StringUtils.isBlank(refreshToken)) {
//            refreshToken = context.getRequest().getHeader(tokenConfig.getRefreshTokenName());
//        }
//        if (StringUtils.isBlank(jwtToken)) {
//            throw new ParserJwtTokenException("当前用户未登录");
//        }
//        // 解析Token得到userId
//        Claims claims;
//        try {
//            claims = JwtTokenUtils.parserJwtToken(securityConfig.getToken(), jwtToken);
//        } catch (ParserJwtTokenException e) {
//            if (!tokenConfig.isEnableRefreshToken() || !(e.getCause() instanceof ExpiredJwtException)) {
//                throw e;
//            }
//            // 验证刷新Token - 重新生成JWT-Token
//            log.debug("开始验证刷新Token | refresh-token={}", refreshToken);
//            if (StringUtils.isBlank(refreshToken)) {
//                throw new InvalidJwtRefreshTokenException("刷新Token为空", e);
//            }
//            // 使用刷新Token创建新的JWT-Token
//            UseJwtRefreshToken useJwtRefreshToken = new UseJwtRefreshToken();
//            claims = JwtTokenUtils.readClaims(jwtToken);
//            useJwtRefreshToken.setUseJwtId(Long.parseLong(claims.getId()));
//            useJwtRefreshToken.setUseRefreshToken(refreshToken);
//            // 创建新的JWT-Token
//            jwtToken = JwtTokenUtils.createJwtToken(tokenConfig, claims);
//            refreshToken = JwtTokenUtils.createRefreshToken(claims.getSubject());
//            useJwtRefreshToken.setJwtId(Long.parseLong(claims.getId()));
//            useJwtRefreshToken.setToken(jwtToken);
//            useJwtRefreshToken.setExpiredTime(claims.getExpiration());
//            useJwtRefreshToken.setRefreshToken(refreshToken);
//            useJwtRefreshToken.setRefreshTokenExpiredTime(new Date(now.getTime() + tokenConfig.getRefreshTokenValidity().toMillis()));
//            // 使用刷新Token
//            NewJwtToken newJwtToken = refreshJwtToken.refresh(useJwtRefreshToken);
//            if (newJwtToken == null) {
//                throw new InvalidJwtRefreshTokenException("无效的刷新Token");
//            }
//            log.debug("刷新Token验证成功 | userId={} | jwt-token={} | refresh-token={}", newJwtToken.getUserId(), newJwtToken.getToken(), newJwtToken.getRefreshToken());
//            // 更新客户端Token数据
//            if (tokenConfig.isUseCookie()) {
//                int tokenMaxAge = DateUtils.pastSeconds(now, newJwtToken.getExpiredTime());
//                int refreshTokenMaxAge = DateUtils.pastSeconds(now, newJwtToken.getRtExpiredTime());
//                int maxAge = Integer.max(refreshTokenMaxAge, tokenMaxAge) + (60 * 3);
//                CookieUtils.setCookie(context.getResponse(), "/", tokenConfig.getJwtTokenName(), newJwtToken.getToken(), maxAge);
//                CookieUtils.setCookie(context.getResponse(), "/", tokenConfig.getRefreshTokenName(), newJwtToken.getRefreshToken(), maxAge);
//            } else {
//                context.getResponse().addHeader(tokenConfig.getJwtTokenName(), newJwtToken.getToken());
//                context.getResponse().addHeader(tokenConfig.getRefreshTokenName(), newJwtToken.getRefreshToken());
//            }
//        }
//        context.setJwtToken(jwtToken);
//        context.setRefreshToken(refreshToken);
//        context.setClaims(claims);
//        context.setUserId(claims.getSubject());
//        context.getRequest().setAttribute(JWT_OBJECT_REQUEST_ATTRIBUTE, claims);
//        JwtTokenHolder.set(claims);
//        // 验证JWT-Token
//        for (VerifyJwtToken verifyJwtToken : verifyJwtTokenList) {
//            verifyJwtToken.verify(jwtToken, context.getUserId(), claims, securityConfig, context.getRequest(), context.getResponse());
//        }
//        // 根据JWT-Token获取SecurityContext
//        SecurityContext securityContext = securityContextRepository.loadContext(context.getUserId(), claims, securityConfig, context.getRequest(), context.getResponse());
//        // 把SecurityContext绑定到当前线程和当前请求对象
//        SecurityContextHolder.setContext(securityContext, context.getRequest());
//        context.setSecurityContext(securityContext);
//    }
//
//    /**
//     * 用户身份认成功处理
//     */
//    protected void authenticationSuccessHandler(AuthenticationContext context) throws Exception {
//        if (authenticationSuccessHandlerList == null) {
//            return;
//        }
//        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(context.getJwtToken(), context.getUserId(), context.getClaims(), context.getSecurityContext());
//        for (AuthenticationSuccessHandler handler : authenticationSuccessHandlerList) {
//            handler.onAuthenticationSuccess(context.getRequest(), context.getResponse(), event);
//        }
//    }
//
//    /**
//     * 用户身份认失败处理
//     */
//    protected void authenticationFailureHandler(AuthenticationContext context) throws Exception {
//        if (authenticationFailureHandlerList == null) {
//            return;
//        }
//        AuthenticationFailureEvent event = new AuthenticationFailureEvent(context.getJwtToken(), context.getUserId(), context.getClaims());
//        for (AuthenticationFailureHandler handler : authenticationFailureHandlerList) {
//            handler.onAuthenticationFailure(context.getRequest(), context.getResponse(), event);
//        }
//    }
//
//    /**
//     * 当认证失败时响应处理
//     */
//    protected void onAuthenticationFailureResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
//        if (response.isCommitted()) {
//            return;
//        }
//        if (securityConfig.isNotLoginNeedRedirect()) {
//            // 需要重定向
//            HttpServletResponseUtils.redirect(response, securityConfig.getNotLoginRedirectPage());
//        } else {
//            // 直接返回
//            HttpServletResponseUtils.sendJson(request, response, HttpStatus.UNAUTHORIZED, e);
//        }
//    }
//}
