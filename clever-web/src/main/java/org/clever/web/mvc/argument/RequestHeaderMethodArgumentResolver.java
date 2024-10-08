package org.clever.web.mvc.argument;

import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.Assert;
import org.clever.web.mvc.annotation.RequestHeader;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * 解析使用 {@code @RequestHeader} 注释的方法参数，{@link Map} 参数除外。
 * 有关使用 {@code @RequestHeader} 注释的 {@link Map} 参数的详细信息，请参阅 {@link RequestHeaderMapMethodArgumentResolver}。
 * <p>{@code @RequestHeader} 是从请求标头解析的命名值。
 * 当请求标头不存在时，它有一个必需的标志和一个默认值可以回退。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/09 21:49 <br/>
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {
    private static final Class<org.springframework.web.bind.annotation.RequestHeader> SPRING_ANNOTATION = org.springframework.web.bind.annotation.RequestHeader.class;

    public RequestHeaderMethodArgumentResolver(boolean useCache) {
        super(useCache);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(RequestHeader.class) || parameter.hasParameterAnnotation(SPRING_ANNOTATION);
        return (hasAnnotation && !Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
    }

    @Override
    protected Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        Enumeration<String> headerValues = request.getHeaders(name);
        if (headerValues != null && headerValues.hasMoreElements()) {
            List<String> list = Collections.list(headerValues);
            return (list.size() == 1 ? list.get(0) : list.toArray(new String[0]));
        } else {
            return null;
        }
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
        if (ann == null) {
            org.springframework.web.bind.annotation.RequestHeader springRequestHeader = parameter.getParameterAnnotation(SPRING_ANNOTATION);
            if (springRequestHeader != null) {
                return new RequestHeaderNamedValueInfo(
                    springRequestHeader.name(),
                    springRequestHeader.required(),
                    springRequestHeader.defaultValue()
                );
            }
        }
        Assert.state(ann != null, "No RequestHeader annotation");
        return new RequestHeaderNamedValueInfo(ann);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
        throw new MissingRequestHeaderException(name, parameter);
    }

    @Override
    protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        throw new MissingRequestHeaderException(name, parameter, true);
    }

    private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {
        private RequestHeaderNamedValueInfo(RequestHeader annotation) {
            super(annotation.name(), annotation.required(), annotation.defaultValue());
        }

        public RequestHeaderNamedValueInfo(String name, boolean required, String defaultValue) {
            super(name, required, defaultValue);
        }
    }
}
