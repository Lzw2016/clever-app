package org.clever.security.authorization;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.OrderComparator;
import org.clever.security.SecurityContextHolder;
import org.clever.security.authorization.voter.AuthorizationVoter;
import org.clever.security.authorization.voter.VoterResult;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.AuthorizationInnerException;
import org.clever.security.handler.AuthorizationFailureHandler;
import org.clever.security.handler.AuthorizationSuccessHandler;
import org.clever.security.model.AuthorizationContext;
import org.clever.security.model.SecurityContext;
import org.clever.security.model.jackson2.event.AuthorizationFailureEvent;
import org.clever.security.model.jackson2.event.AuthorizationSuccessEvent;
import org.clever.security.model.response.ForbiddenAccessRes;
import org.clever.security.utils.HttpServletResponseUtils;
import org.clever.security.utils.PathFilterUtils;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 用户权限认证拦截器(授权拦截器)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:18 <br/>
 */
@Slf4j
public class AuthorizationFilter implements FilterRegistrar.FilterFuc {
    /**
     * 全局配置
     */
    private final SecurityConfig securityConfig;
    /**
     * 授权投票器
     */
    private final List<AuthorizationVoter> authorizationVoterList;
    /**
     * 授权成功的处理
     */
    private final List<AuthorizationSuccessHandler> authorizationSuccessHandlerList;
    /**
     * 授权失败的处理
     */
    private final List<AuthorizationFailureHandler> authorizationFailureHandlerList;

    public AuthorizationFilter(
            SecurityConfig securityConfig,
            List<AuthorizationVoter> authorizationVoterList,
            List<AuthorizationSuccessHandler> authorizationSuccessHandlerList,
            List<AuthorizationFailureHandler> authorizationFailureHandlerList) {
        Assert.notNull(securityConfig, "权限系统配置对象(SecurityConfig)不能为null");
        Assert.notNull(authorizationVoterList, "授权投票器(AuthorizationVoter)不能为null");
        Assert.notNull(authorizationSuccessHandlerList, "授权成功的处理(AuthorizationSuccessHandler)不能为null");
        Assert.notNull(authorizationFailureHandlerList, "授权成功的处理(AuthorizationFailureHandler)不能为null");
        OrderComparator.sort(authorizationVoterList);
        OrderComparator.sort(authorizationSuccessHandlerList);
        OrderComparator.sort(authorizationFailureHandlerList);
        this.securityConfig = securityConfig;
        this.authorizationVoterList = authorizationVoterList;
        this.authorizationSuccessHandlerList = authorizationSuccessHandlerList;
        this.authorizationFailureHandlerList = authorizationFailureHandlerList;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        // 不需要授权
        if (!PathFilterUtils.isAuthorizationRequest(ctx.req, securityConfig)) {
            ctx.next();
            return;
        }
        log.debug("### 开始执行授权逻辑 ---------------------------------------------------------------------->");
        // 执行授权逻辑
        AuthorizationContext context = new AuthorizationContext(ctx.req, ctx.res);
        boolean pass;
        try {
            // 执行授权流程
            pass = authorization(context);
            log.debug("### 授权完成，结果: [{}]", pass ? "通过" : "拒绝");
        } catch (Throwable e) {
            // 认证或授权失败时不拦截的请求
            if (PathFilterUtils.isIgnoreAuthFailedRequest(context.getRequest(), securityConfig)) {
                ctx.next();
                return;
            }
            // 授权异常
            log.error("授权异常", e);
            HttpServletResponseUtils.sendData(ctx.req, ctx.res, HttpServletResponseUtils.getHttpStatus(e), e);
            return;
        } finally {
            log.debug("### 授权逻辑执行完成 <----------------------------------------------------------------------");
        }
        // 执行授权事件
        try {
            if (pass) {
                onAuthorizationSuccess(context);
            } else {
                onAuthorizationFailure(context);
                // 无权访问 403
                onAuthorizationFailureResponse(context);
            }
        } catch (Throwable e) {
            log.error("授权异常", e);
            HttpServletResponseUtils.sendData(ctx.req, ctx.res, HttpStatus.INTERNAL_SERVER_ERROR, e);
            return;
        }
        // 处理业务逻辑
        if (pass) {
            ctx.next();
        }
    }

    /**
     * 授权流程
     */
    protected boolean authorization(AuthorizationContext context) {
        SecurityContext securityContext = SecurityContextHolder.getContext(context.getRequest());
        if (securityContext == null) {
            throw new AuthorizationInnerException("获取SecurityContext失败");
        }
        context.setSecurityContext(securityContext);
        // 开始授权
        long passWeight = 0;
        for (AuthorizationVoter authorizationVoter : authorizationVoterList) {
            if (!authorizationVoter.isSupported(securityConfig, context.getRequest(), securityContext)) {
                continue;
            }
            VoterResult voterResult = authorizationVoter.vote(securityConfig, context.getRequest(), securityContext);
            if (voterResult == null) {
                context.setAuthorizationException(new AuthorizationInnerException("授权投票结果为null"));
                throw context.getAuthorizationException();
            } else if (Objects.equals(VoterResult.PASS.getId(), voterResult.getId())) {
                // 通过
                passWeight = passWeight + authorizationVoter.getWeight();
                log.debug(
                        "### 授权通过 | PassWeight={} | Weight={} | Voter={}",
                        passWeight, authorizationVoter.getWeight(), authorizationVoter.getClass().getSimpleName()
                );
            } else if (Objects.equals(VoterResult.REJECT.getId(), voterResult.getId())) {
                // 驳回
                passWeight = passWeight - authorizationVoter.getWeight();
                log.debug(
                        "### 授权驳回 | PassWeight={} | Weight={} | Voter={}",
                        passWeight, authorizationVoter.getWeight(), authorizationVoter.getClass().getSimpleName()
                );
            } else if (Objects.equals(VoterResult.ABSTAIN.getId(), voterResult.getId())) {
                // 弃权
                log.debug(
                        "### 放弃授权 | PassWeight={} | Weight={} | Voter={}",
                        passWeight, authorizationVoter.getWeight(), authorizationVoter.getClass().getSimpleName()
                );
            } else {
                throw new AuthorizationInnerException("未知的授权投票结果");
            }
        }
        return passWeight >= 0;
    }

    /**
     * 当授权成功时处理
     */
    protected void onAuthorizationSuccess(AuthorizationContext context) {
        SecurityContext securityContext = context.getSecurityContext();
        AuthorizationSuccessEvent event = new AuthorizationSuccessEvent(
                securityContext.getUserInfo(), securityContext.getRoles(), securityContext.getPermissions()
        );
        for (AuthorizationSuccessHandler handler : authorizationSuccessHandlerList) {
            handler.onAuthorizationSuccess(context.getRequest(), context.getResponse(), event);
        }
    }

    /**
     * 当授权失败时处理
     */
    protected void onAuthorizationFailure(AuthorizationContext context) {
        SecurityContext securityContext = context.getSecurityContext();
        AuthorizationFailureEvent event = new AuthorizationFailureEvent(
                securityContext.getUserInfo(), securityContext.getRoles(), securityContext.getPermissions()
        );
        for (AuthorizationFailureHandler handler : authorizationFailureHandlerList) {
            handler.onAuthorizationFailure(context.getRequest(), context.getResponse(), event);
        }
    }

    /**
     * 当授权失败时响应处理
     */
    protected void onAuthorizationFailureResponse(AuthorizationContext context) throws IOException {
        HttpServletResponse response = context.getResponse();
        if (response.isCommitted()) {
            return;
        }
        if (securityConfig.isForbiddenNeedRedirect()) {
            // 需要重定向
            HttpServletResponseUtils.redirect(response, securityConfig.getNotLoginRedirectPage());
        } else {
            // 直接返回
            ForbiddenAccessRes forbiddenAccessRes = new ForbiddenAccessRes("未授权，禁止访问");
            HttpServletResponseUtils.sendData(response, forbiddenAccessRes, HttpStatus.FORBIDDEN);
        }
    }
}
