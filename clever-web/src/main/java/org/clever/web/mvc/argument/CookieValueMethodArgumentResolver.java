package org.clever.web.mvc.argument;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.Assert;
import org.clever.core.http.CookieUtils;
import org.clever.web.mvc.annotation.CookieValue;
import org.clever.web.utils.WebUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.ServletRequestBindingException;

/**
 * 用于解析用 {@code @CookieValue} 注释的方法参数。从 {@link HttpServletRequest} 请求中提取 cookie 值
 * <p>{@code @CookieValue} 是从 cookie 解析的命名值。
 * 当 cookie 不存在时，它有一个必需的标志和一个默认值可以回退。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:57 <br/>
 */
public class CookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {
    public CookieValueMethodArgumentResolver(boolean useCache) {
        super(useCache);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, HttpServletRequest request) {
        return parameter.hasParameterAnnotation(CookieValue.class);
    }

    @Override
    protected Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        Cookie cookieValue = WebUtils.getCookie(request, name);
        if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
            return cookieValue;
        } else if (cookieValue != null) {
            return CookieUtils.decodeRequestString(request, cookieValue.getValue());
        } else {
            return null;
        }
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
        Assert.state(annotation != null, "No CookieValue annotation");
        return new CookieValueNamedValueInfo(annotation);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
        throw new MissingRequestCookieException(name, parameter);
    }

    @Override
    protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        throw new MissingRequestCookieException(name, parameter, true);
    }

    private static final class CookieValueNamedValueInfo extends NamedValueInfo {
        private CookieValueNamedValueInfo(CookieValue annotation) {
            super(annotation.name(), annotation.required(), annotation.defaultValue());
        }
    }
}
