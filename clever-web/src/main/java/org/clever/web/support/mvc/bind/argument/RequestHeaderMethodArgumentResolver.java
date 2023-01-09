package org.clever.web.support.mvc.bind.argument;

import org.clever.core.MethodParameter;
import org.clever.util.Assert;
import org.clever.web.exception.MissingRequestHeaderException;
import org.clever.web.exception.ServletRequestBindingException;
import org.clever.web.support.mvc.bind.annotation.RequestHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Resolves method arguments annotated with {@code @RequestHeader} except for
 * {@link Map} arguments. See {@link RequestHeaderMapMethodArgumentResolver} for
 * details on {@link Map} arguments annotated with {@code @RequestHeader}.
 *
 * <p>An {@code @RequestHeader} is a named value resolved from a request header.
 * It has a required flag and a default value to fall back on when the request
 * header does not exist.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved
 * request header values that don't yet match the method parameter type.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/09 21:49 <br/>
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {
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
