package org.clever.web.support.mvc.argument;

import org.clever.core.MethodParameter;
import org.clever.util.Assert;
import org.clever.web.exception.MissingRequestHeaderException;
import org.clever.web.exception.ServletRequestBindingException;
import org.clever.web.support.mvc.annotation.RequestHeader;

import javax.servlet.http.HttpServletRequest;
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
    public RequestHeaderMethodArgumentResolver() {
        super();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return (parameter.hasParameterAnnotation(RequestHeader.class) && !Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
    }

    @Override
    protected Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        Enumeration<String> headerValues = request.getHeaders(name);
        if (headerValues != null) {
            List<String> list = Collections.list(headerValues);
            return (list.size() == 1 ? list.get(0) : list.toArray(new String[0]));
        } else {
            return null;
        }
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
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
    }
}
