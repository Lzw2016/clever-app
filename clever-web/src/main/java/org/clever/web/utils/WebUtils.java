package org.clever.web.utils;

import org.clever.util.Assert;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

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

    /**
     * 返回包含具有给定前缀的所有参数的映射。将单个值映射到字符串，将多个值映射到字符串数组。
     * <p>例如，前缀为“spring_”、“spring_param1”和“spring_param2”会导致以“param1”和“param2”作为键的映射。
     *
     * @param request 要在其中查找参数的 HTTP 请求
     * @param prefix  参数名称的开头（如果这是 null 或空字符串，则所有参数都将匹配）
     * @return 包含请求参数<b>没有前缀</b>的映射，包含一个字符串或一个字符串数组作为值
     * @see javax.servlet.ServletRequest#getParameterNames
     * @see javax.servlet.ServletRequest#getParameterValues
     * @see javax.servlet.ServletRequest#getParameterMap
     */
    public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
        Assert.notNull(request, "Request must not be null");
        Enumeration<String> paramNames = request.getParameterNames();
        Map<String, Object> params = new TreeMap<>();
        if (prefix == null) {
            prefix = "";
        }
        while (paramNames != null && paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (prefix.isEmpty() || paramName.startsWith(prefix)) {
                String unprefixed = paramName.substring(prefix.length());
                String[] values = request.getParameterValues(paramName);
                // noinspection StatementWithEmptyBody
                if (values == null || values.length == 0) {
                    // 什么都不做，根本找不到任何值
                } else if (values.length > 1) {
                    params.put(unprefixed, values);
                } else {
                    params.put(unprefixed, values[0]);
                }
            }
        }
        return params;
    }
}
