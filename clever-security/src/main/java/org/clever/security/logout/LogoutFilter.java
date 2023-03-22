package org.clever.security.logout;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.OrderComparator;
import org.clever.core.http.CookieUtils;
import org.clever.security.SecurityContextHolder;
import org.clever.security.authentication.AuthenticationFilter;
import org.clever.security.config.LogoutConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.config.TokenConfig;
import org.clever.security.exception.LogoutException;
import org.clever.security.exception.LogoutFailedException;
import org.clever.security.exception.UnSupportLogoutException;
import org.clever.security.handler.LogoutFailureHandler;
import org.clever.security.handler.LogoutSuccessHandler;
import org.clever.security.model.LogoutContext;
import org.clever.security.model.SecurityContext;
import org.clever.security.model.jackson2.event.LogoutFailureEvent;
import org.clever.security.model.jackson2.event.LogoutSuccessEvent;
import org.clever.security.utils.HttpRespondHandler;
import org.clever.security.utils.PathFilterUtils;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.http.HttpStatus;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:35 <br/>
 */
@Slf4j
public class LogoutFilter implements FilterRegistrar.FilterFuc {
    /**
     * 全局配置
     */
    private final SecurityConfig securityConfig;
    /**
     * 登出成功处理
     */
    private final List<LogoutSuccessHandler> logoutSuccessHandlerList;
    /**
     * 登出失败处理
     */
    private final List<LogoutFailureHandler> logoutFailureHandlerList;
    /**
     * 返回响应数据工具
     */
    public final HttpRespondHandler httpRespondHandler;

    public LogoutFilter(
            SecurityConfig securityConfig,
            List<LogoutSuccessHandler> logoutSuccessHandlerList,
            List<LogoutFailureHandler> logoutFailureHandlerList,
            HttpRespondHandler httpRespondHandler) {
        Assert.notNull(securityConfig, "权限系统配置对象(SecurityConfig)不能为null");
        Assert.notNull(httpRespondHandler, "返回响应数据工具(httpRespondHandler)不能为null");
        OrderComparator.sort(logoutSuccessHandlerList);
        OrderComparator.sort(logoutFailureHandlerList);
        this.securityConfig = securityConfig;
        this.logoutSuccessHandlerList = logoutSuccessHandlerList;
        this.logoutFailureHandlerList = logoutFailureHandlerList;
        this.httpRespondHandler = httpRespondHandler;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        if (!securityConfig.isEnable()) {
            ctx.next();
            return;
        }
        // 不是登出请求
        if (!PathFilterUtils.isLogoutRequest(ctx.req, securityConfig)) {
            ctx.next();
            return;
        }
        log.debug("### 开始执行登出逻辑 ---------------------------------------------------------------------->");
        log.debug("当前请求 -> [{}]", ctx.req.getRequestURI());
        // 执行登出逻辑
        LogoutContext context = new LogoutContext(ctx.req, ctx.res);
        try {
            logout(context);
            // 登出成功 - 返回数据给客户端
            onLogoutSuccessResponse(context);
        } catch (Throwable e) {
            // 登出异常
            log.error("登出异常", e);
            if (context.isSuccess()) {
                onLogoutSuccessResponse(context);
            } else {
                onLogoutFailureResponse(context, e);
            }
        } finally {
            log.debug("### 登出逻辑执行完成 <----------------------------------------------------------------------");
        }
    }

    protected void logout(LogoutContext context) throws Exception {
        context.setSecurityContext(SecurityContextHolder.getContext(context.getRequest()));
        if (context.getSecurityContext() == null) {
            context.setLogoutException(new UnSupportLogoutException("当前未登录,无法登出"));
            throw context.getLogoutException();
        }
        // 删除JWT-Token  LogoutContext
        try {
            TokenConfig tokenConfig = securityConfig.getToken();
            CookieUtils.delCookieForRooPath(context.getRequest(), context.getResponse(), tokenConfig.getJwtTokenName());
            if (tokenConfig.isEnableRefreshToken()) {
                CookieUtils.delCookieForRooPath(context.getRequest(), context.getResponse(), tokenConfig.getRefreshTokenName());
            }
            log.debug("### 删除JWT-Token成功");
            context.setSuccess(true);
        } catch (Exception e) {
            log.debug("### 删除JWT-Token失败", e);
            context.setLogoutException(new LogoutFailedException("登出失败", e));
        }
        // 登出成功
        if (context.getLogoutException() == null && logoutSuccessHandlerList != null) {
            Claims claims = (Claims) context.getRequest().getAttribute(AuthenticationFilter.JWT_OBJECT_REQUEST_ATTRIBUTE);
            LogoutSuccessEvent logoutSuccessEvent = new LogoutSuccessEvent(context.getSecurityContext(), claims);
            for (LogoutSuccessHandler handler : logoutSuccessHandlerList) {
                handler.onLogoutSuccess(context.getRequest(), context.getResponse(), logoutSuccessEvent);
            }
        }
        // 登出失败
        if (context.getLogoutException() != null && logoutFailureHandlerList != null) {
            LogoutFailureEvent logoutFailureEvent = new LogoutFailureEvent(context.getLogoutException());
            for (LogoutFailureHandler handler : logoutFailureHandlerList) {
                handler.onLogoutFailure(context.getRequest(), context.getResponse(), logoutFailureEvent);
            }
        }
        if (context.getLogoutException() != null) {
            throw context.getLogoutException();
        }
    }

    /**
     * 当登出成功时响应处理
     */
    protected void onLogoutSuccessResponse(LogoutContext context) throws IOException {
        if (context.getResponse().isCommitted()) {
            return;
        }
        LogoutConfig logout = securityConfig.getLogout();
        if (logout != null && logout.isNeedRedirect()) {
            // 需要重定向
            httpRespondHandler.redirect(context.getResponse(), logout.getRedirectPage());
        } else {
            // 直接返回
            SecurityContext securityContext = SecurityContextHolder.getContext(context.getRequest());
            if (securityContext == null) {
                throw new UnSupportLogoutException("当前未登录,无法登出");
            }
            httpRespondHandler.logoutSuccess(context);
        }
    }

    /**
     * 当登出失败时响应处理
     */
    protected void onLogoutFailureResponse(LogoutContext context, Throwable e) throws IOException {
        if (context.getResponse().isCommitted()) {
            return;
        }
        LogoutConfig logout = securityConfig.getLogout();
        if (logout != null && logout.isNeedRedirect()) {
            // 需要重定向
            httpRespondHandler.redirect(context.getResponse(), logout.getRedirectPage());
        } else {
            // 直接返回
            if (e instanceof LogoutException) {
                context.setLogoutException((LogoutException) e);
                httpRespondHandler.logoutFailure(context);
            } else {
                httpRespondHandler.sendData(context.getRequest(), context.getResponse(), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
}
