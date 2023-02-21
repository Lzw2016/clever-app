//package org.clever.security.logout;
//
//import org.clever.core.http.CookieUtils;
//import org.clever.security.SecurityContextHolder;
//import org.clever.security.config.LogoutConfig;
//import org.clever.security.config.SecurityConfig;
//import org.clever.security.config.TokenConfig;
//import org.clever.security.exception.LogoutException;
//import org.clever.security.exception.LogoutFailedException;
//import org.clever.security.exception.UnSupportLogoutException;
//import org.clever.security.handler.LogoutFailureHandler;
//import org.clever.security.handler.LogoutSuccessHandler;
//import org.clever.security.model.LogoutContext;
//import org.clever.security.model.SecurityContext;
//import org.clever.security.model.jackson2.event.LogoutFailureEvent;
//import org.clever.security.model.jackson2.event.LogoutSuccessEvent;
//import org.clever.security.model.response.LogoutRes;
//import org.clever.security.utils.HttpServletResponseUtils;
//import org.clever.security.utils.ListSortUtils;
//import org.clever.security.utils.PathFilterUtils;
//import io.jsonwebtoken.Claims;
//import lombok.extern.slf4j.Slf4j;
//import org.clever.util.Assert;
//import org.clever.web.http.HttpStatus;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpFilter;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.List;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2020/11/29 16:35 <br/>
// */
//@Slf4j
//public class LogoutFilter extends HttpFilter {
//    /**
//     * 全局配置
//     */
//    private final SecurityConfig securityConfig;
//    /**
//     * 登出成功处理
//     */
//    private final List<LogoutSuccessHandler> logoutSuccessHandlerList;
//    /**
//     * 登出失败处理
//     */
//    private final List<LogoutFailureHandler> logoutFailureHandlerList;
//
//    public LogoutFilter(
//            SecurityConfig securityConfig,
//            List<LogoutSuccessHandler> logoutSuccessHandlerList,
//            List<LogoutFailureHandler> logoutFailureHandlerList) {
//        Assert.notNull(securityConfig, "权限系统配置对象(SecurityConfig)不能为null");
//        this.securityConfig = securityConfig;
//        this.logoutSuccessHandlerList = ListSortUtils.sort(logoutSuccessHandlerList);
//        this.logoutFailureHandlerList = ListSortUtils.sort(logoutFailureHandlerList);
//    }
//
//    @Override
//    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
////        // 是否是跨域请求
////        if (PathFilterUtils.isPreFlightRequest(request, response)) {
////            return;
////        }
//        // 不是登出请求
//        if (!PathFilterUtils.isLogoutRequest(request, securityConfig)) {
//            chain.doFilter(request, response);
//            return;
//        }
//        log.debug("### 开始执行登出逻辑 ---------------------------------------------------------------------->");
//        log.debug("当前请求 -> [{}]", request.getRequestURI());
//        // 执行登出逻辑
//        LogoutContext context = new LogoutContext(request, response);
//        try {
//            logout(context);
//            // 登出成功 - 返回数据给客户端
//            onLogoutSuccessResponse(request, response);
//        } catch (Throwable e) {
//            // 登出异常
//            log.error("登出异常", e);
//            if (context.isSuccess()) {
//                onLogoutSuccessResponse(request, response);
//            } else {
//                onLogoutFailureResponse(request, response, e);
//            }
//        } finally {
//            log.debug("### 登出逻辑执行完成 <----------------------------------------------------------------------");
//        }
//    }
//
//    protected void logout(LogoutContext context) throws Exception {
//        context.setSecurityContext(SecurityContextHolder.getContext(context.getRequest()));
//        if (context.getSecurityContext() == null) {
//            context.setLogoutException(new UnSupportLogoutException("当前未登录,无法登出"));
//            throw context.getLogoutException();
//        }
//        // 删除JWT-Token  LogoutContext
//        try {
//            TokenConfig tokenConfig = securityConfig.getToken();
//            CookieUtils.delCookieForRooPath(context.getRequest(), context.getResponse(), tokenConfig.getJwtTokenName());
//            if (tokenConfig.isEnableRefreshToken()) {
//                CookieUtils.delCookieForRooPath(context.getRequest(), context.getResponse(), tokenConfig.getRefreshTokenName());
//            }
//            log.debug("### 删除JWT-Token成功");
//            context.setSuccess(true);
//        } catch (Exception e) {
//            log.debug("### 删除JWT-Token失败", e);
//            context.setLogoutException(new LogoutFailedException("登出失败", e));
//        }
//        // 登出成功
//        if (context.getLogoutException() == null && logoutSuccessHandlerList != null) {
//            Claims claims = (Claims) context.getRequest().getAttribute(AuthenticationFilter.JWT_OBJECT_REQUEST_ATTRIBUTE);
//            LogoutSuccessEvent logoutSuccessEvent = new LogoutSuccessEvent(context.getSecurityContext(), claims);
//            for (LogoutSuccessHandler handler : logoutSuccessHandlerList) {
//                handler.onLogoutSuccess(context.getRequest(), context.getResponse(), logoutSuccessEvent);
//            }
//        }
//        // 登出失败
//        if (context.getLogoutException() != null && logoutFailureHandlerList != null) {
//            LogoutFailureEvent logoutFailureEvent = new LogoutFailureEvent(context.getLogoutException());
//            for (LogoutFailureHandler handler : logoutFailureHandlerList) {
//                handler.onLogoutFailure(context.getRequest(), context.getResponse(), logoutFailureEvent);
//            }
//        }
//        if (context.getLogoutException() != null) {
//            throw context.getLogoutException();
//        }
//    }
//
//    /**
//     * 当登出成功时响应处理
//     */
//    protected void onLogoutSuccessResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        if (response.isCommitted()) {
//            return;
//        }
//        LogoutConfig logout = securityConfig.getLogout();
//        if (logout != null && logout.isNeedRedirect()) {
//            // 需要重定向
//            HttpServletResponseUtils.redirect(response, logout.getRedirectPage());
//        } else {
//            // 直接返回
//            SecurityContext securityContext = SecurityContextHolder.getContext(request);
//            if (securityContext == null) {
//                throw new UnSupportLogoutException("当前未登录,无法登出");
//            }
//            LogoutRes loginRes = LogoutRes.logoutSuccess(securityContext.getUserInfo());
//            HttpServletResponseUtils.sendJson(response, loginRes);
//        }
//    }
//
//    /**
//     * 当登出失败时响应处理
//     */
//    protected void onLogoutFailureResponse(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
//        if (response.isCommitted()) {
//            return;
//        }
//        LogoutConfig logout = securityConfig.getLogout();
//        if (logout != null && logout.isNeedRedirect()) {
//            // 需要重定向
//            HttpServletResponseUtils.redirect(response, logout.getRedirectPage());
//        } else {
//            // 直接返回
//            if (e instanceof LogoutException) {
//                LogoutRes loginRes = LogoutRes.logoutFailure(e.getMessage());
//                HttpServletResponseUtils.sendJson(response, loginRes);
//            } else {
//                HttpServletResponseUtils.sendJson(request, response, HttpStatus.INTERNAL_SERVER_ERROR, e);
//            }
//        }
//    }
//}
