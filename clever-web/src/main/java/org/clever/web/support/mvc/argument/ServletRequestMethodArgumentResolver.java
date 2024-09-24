package org.clever.web.support.mvc.argument;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.PushBuilder;
import org.clever.web.utils.WebUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;

/**
 * 解析 servlet 支持的与请求相关的方法参数。支持以下类型的值：
 * <ul>
 * <li>{@link ServletRequest}
 * <li>{@link MultipartRequest}
 * <li>{@link HttpSession}
 * <li>{@link PushBuilder} （从 Servlet 4.0 上）
 * <li>{@link Principal} 但只有在未注释的情况下才能允许自定义解析器解析它，并且回退到 {@link PrincipalMethodArgumentResolver}
 * <li>{@link InputStream}
 * <li>{@link Reader}
 * <li>{@link HttpMethod}
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/14 10:59 <br/>
 */
public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        Class<?> paramType = parameter.getParameterType();
        return (ServletRequest.class.isAssignableFrom(paramType)
            || MultipartRequest.class.isAssignableFrom(paramType)
            || HttpSession.class.isAssignableFrom(paramType)
            || PushBuilder.class.isAssignableFrom(paramType)
            || (Principal.class.isAssignableFrom(paramType) && !parameter.hasParameterAnnotations())
            || InputStream.class.isAssignableFrom(paramType)
            || Reader.class.isAssignableFrom(paramType)
            || HttpMethod.class == paramType);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Class<?> paramType = parameter.getParameterType();
        // ServletRequest / HttpServletRequest / MultipartRequest / MultipartHttpServletRequest
        if (ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType)) {
            return resolveNativeRequest(request, paramType);
        }
        // HttpServletRequest required for all further argument types
        return resolveArgument(paramType, resolveNativeRequest(request, HttpServletRequest.class));
    }

    private <T> T resolveNativeRequest(HttpServletRequest request, Class<T> requiredType) {
        T nativeRequest = WebUtils.getNativeRequest(request, requiredType);
        if (nativeRequest == null) {
            throw new IllegalStateException("Current request is not of type [" + requiredType.getName() + "]: " + request);
        }
        return nativeRequest;
    }

    private Object resolveArgument(Class<?> paramType, HttpServletRequest request) throws IOException {
        if (HttpSession.class.isAssignableFrom(paramType)) {
            HttpSession session = request.getSession();
            if (session != null && !paramType.isInstance(session)) {
                throw new IllegalStateException("Current session is not of type [" + paramType.getName() + "]: " + session);
            }
            return session;
        } else if (PushBuilder.class.isAssignableFrom(paramType)) {
            return PushBuilderDelegate.resolvePushBuilder(request, paramType);
        } else if (InputStream.class.isAssignableFrom(paramType)) {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null && !paramType.isInstance(inputStream)) {
                throw new IllegalStateException("Request input stream is not of type [" + paramType.getName() + "]: " + inputStream);
            }
            return inputStream;
        } else if (Reader.class.isAssignableFrom(paramType)) {
            Reader reader = request.getReader();
            if (reader != null && !paramType.isInstance(reader)) {
                throw new IllegalStateException("Request body reader is not of type [" + paramType.getName() + "]: " + reader);
            }
            return reader;
        } else if (Principal.class.isAssignableFrom(paramType)) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null && !paramType.isInstance(userPrincipal)) {
                throw new IllegalStateException("Current user principal is not of type [" + paramType.getName() + "]: " + userPrincipal);
            }
            return userPrincipal;
        } else if (HttpMethod.class == paramType) {
            return HttpMethod.valueOf(request.getMethod());
        }
        // Should never happen...
        throw new UnsupportedOperationException("Unknown parameter type: " + paramType.getName());
    }

    /**
     * 内部类以避免在运行时对 Servlet API 4.0 的硬依赖
     */
    private static class PushBuilderDelegate {
        public static Object resolvePushBuilder(HttpServletRequest request, Class<?> paramType) {
            PushBuilder pushBuilder = request.newPushBuilder();
            if (pushBuilder != null && !paramType.isInstance(pushBuilder)) {
                throw new IllegalStateException("Current push builder is not of type [" + paramType.getName() + "]: " + pushBuilder);
            }
            return pushBuilder;
        }
    }
}
