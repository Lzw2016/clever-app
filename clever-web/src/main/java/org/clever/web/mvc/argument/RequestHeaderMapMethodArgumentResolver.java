package org.clever.web.mvc.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.web.mvc.annotation.RequestHeader;
import org.springframework.core.MethodParameter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析用 {@code @RequestHeader} 注释的 {@link Map} 方法参数。
 * 对于用 {@code @RequestHeader} 注释的单个标头值，请参阅 {@link RequestHeaderMethodArgumentResolver}。
 *
 * <p>创建的 {@link Map} 包含所有请求标头/名称值对。
 * 方法参数类型可以是 {@link MultiValueMap} 以接收标头的所有值，而不仅仅是第一个。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 23:03 <br/>
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private static final Class<org.springframework.web.bind.annotation.RequestHeader> SPRING_ANNOTATION = org.springframework.web.bind.annotation.RequestHeader.class;

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(RequestHeader.class) || parameter.hasParameterAnnotation(SPRING_ANNOTATION);
        return (hasAnnotation && Map.class.isAssignableFrom(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Class<?> paramType = parameter.getParameterType();
        if (MultiValueMap.class.isAssignableFrom(paramType)) {
            MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
            for (Enumeration<String> iterator = request.getHeaderNames(); iterator.hasMoreElements(); ) {
                String headerName = iterator.nextElement();
                Enumeration<String> headerValues = request.getHeaders(headerName);
                if (headerValues != null) {
                    result.addAll(headerName, Collections.list(headerValues));
                }
            }
            return result;
        } else {
            Map<String, String> result = new LinkedHashMap<>();
            for (Enumeration<String> iterator = request.getHeaderNames(); iterator.hasMoreElements(); ) {
                String headerName = iterator.nextElement();
                String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    result.put(headerName, headerValue);
                }
            }
            return result;
        }
    }
}
