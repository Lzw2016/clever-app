package org.clever.web.mvc.argument;

import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.web.utils.WebUtils;
import org.springframework.core.MethodParameter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * 解析 servlet 支持的与响应相关的方法参数。支持以下类型的值：
 * <ul>
 * <li>{@link ServletResponse}
 * <li>{@link OutputStream}
 * <li>{@link Writer}
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/14 11:21 <br/>
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        Class<?> paramType = parameter.getParameterType();
        return (ServletResponse.class.isAssignableFrom(paramType)
            || OutputStream.class.isAssignableFrom(paramType)
            || Writer.class.isAssignableFrom(paramType));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Class<?> paramType = parameter.getParameterType();
        // ServletResponse, HttpServletResponse
        if (ServletResponse.class.isAssignableFrom(paramType)) {
            return resolveNativeResponse(response, paramType);
        }
        // ServletResponse required for all further argument types
        return resolveArgument(paramType, resolveNativeResponse(response, ServletResponse.class));
    }

    private <T> T resolveNativeResponse(HttpServletResponse response, Class<T> requiredType) {
        T nativeResponse = WebUtils.getNativeResponse(response, requiredType);
        if (nativeResponse == null) {
            throw new IllegalStateException("Current response is not of type [" + requiredType.getName() + "]: " + response);
        }
        return nativeResponse;
    }

    private Object resolveArgument(Class<?> paramType, ServletResponse response) throws IOException {
        if (OutputStream.class.isAssignableFrom(paramType)) {
            return response.getOutputStream();
        } else if (Writer.class.isAssignableFrom(paramType)) {
            return response.getWriter();
        }
        // Should never happen...
        throw new UnsupportedOperationException("Unknown parameter type: " + paramType);
    }
}
