package org.clever.web.utils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;

/**
 * Web 应用程序的其他实用程序。由各种框架类使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/11 16:18 <br/>
 */
public abstract class WebUtils {
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
}
