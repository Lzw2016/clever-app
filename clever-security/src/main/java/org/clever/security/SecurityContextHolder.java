package org.clever.security;

import org.clever.security.model.SecurityContext;
import org.clever.security.model.UserInfo;
import org.clever.util.Assert;

import javax.servlet.http.HttpServletRequest;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 21:23 <br/>
 */
public class SecurityContextHolder {
    public static final String SECURITY_CONTEXT_ATTRIBUTE = SecurityContextHolder.class.getName() + "_Security_Context_Attribute";
    private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前安全上下文(用户信息)
     *
     * @param securityContext 安全上下文(用户信息)
     * @param request         当前请求对象
     */
    public static void setContext(SecurityContext securityContext, HttpServletRequest request) {
        Assert.notNull(securityContext, "参数 securityContext 不能为 null");
        Assert.notNull(request, "参数 request 不能为 null");
        request.setAttribute(SECURITY_CONTEXT_ATTRIBUTE, securityContext);
        SECURITY_CONTEXT.set(securityContext);
    }

    /**
     * 清除当前安全上下文(用户信息)
     */
    public static void clearContext() {
        SECURITY_CONTEXT.remove();
    }

    /**
     * 当前请求是否包含安全上下文(用户信息)
     *
     * @param request 请求对象
     */
    public static boolean containsContext(HttpServletRequest request) {
        Assert.notNull(request, "参数 request 不能为 null");
        Object securityContext = request.getAttribute(SECURITY_CONTEXT_ATTRIBUTE);
        return (securityContext instanceof SecurityContext);
    }

    /**
     * 获取当前安全上下文(用户信息)
     */
    public static SecurityContext getContext() {
        return SECURITY_CONTEXT.get();
    }

    /**
     * 获取当前安全上下文(用户信息)
     *
     * @param request 请求对象
     */
    public static SecurityContext getContext(HttpServletRequest request) {
        Assert.notNull(request, "参数 request 不能为 null");
        Object securityContext = request.getAttribute(SECURITY_CONTEXT_ATTRIBUTE);
        if (securityContext instanceof SecurityContext) {
            return (SecurityContext) securityContext;
        } else {
            return null;
        }
    }

    /**
     * 获取当前用户信息
     */
    public static UserInfo getUserInfo() {
        SecurityContext securityContext = getContext();
        if (securityContext == null) {
            return null;
        }
        return securityContext.getUserInfo();
    }
}
