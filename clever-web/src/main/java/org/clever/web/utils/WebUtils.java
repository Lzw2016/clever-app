package org.clever.web.utils;

import org.clever.util.Assert;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Web 应用程序的其他实用程序。由各种框架类使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/11 16:18 <br/>
 */
public abstract class WebUtils {
    /**
     * 根据 Servlet 规范，当 {@code request.getCharacterEncoding} 返回 {@code null} 时使用的默认字符编码。
     *
     * @see ServletRequest#getCharacterEncoding
     */
    public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

    /**
     * 返回指定类型的适当请求对象（如果可用），并根据需要解包给定请求。
     *
     * @param request      servlet 请求
     * @param requiredType 所需的请求对象类型
     * @return 匹配的请求对象，或者 {@code null} 如果该类型不可用
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNativeRequest(ServletRequest request, Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(request)) {
                return (T) request;
            } else if (request instanceof ServletRequestWrapper) {
                return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
            }
        }
        return null;
    }

    /**
     * 返回指定类型的适当响应对象（如果可用），并根据需要解包给定的响应。
     *
     * @param response     servlet 响应
     * @param requiredType 所需的响应对象类型
     * @return 匹配的响应对象，或者 {@code null} 如果该类型不可用
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNativeResponse(ServletResponse response, Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(response)) {
                return (T) response;
            } else if (response instanceof ServletResponseWrapper) {
                return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
            }
        }
        return null;
    }

    /**
     * 检索具有给定名称的第一个 cookie。
     * 请注意，多个 cookie 可以具有相同的名称但不同的路径或域。
     *
     * @param request 当前 servlet 请求
     * @param name    cookie 名称
     * @return 具有给定名称的第一个 cookie，如果找不到，则为 {@code null}
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        Assert.notNull(request, "Request must not be null");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
}
