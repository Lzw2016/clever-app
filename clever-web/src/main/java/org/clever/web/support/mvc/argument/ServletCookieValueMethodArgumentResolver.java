package org.clever.web.support.mvc.argument;

import org.clever.core.MethodParameter;
import org.clever.web.utils.UrlPathHelper;
import org.clever.web.utils.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


/**
 * {@link AbstractCookieValueMethodArgumentResolver} 从 {@link HttpServletRequest} 解析 cookie 值
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:57 <br/>
 */
public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {
    private final UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

    public ServletCookieValueMethodArgumentResolver() {
        super();
    }

    @Override
    protected Object resolveValue(String name, MethodParameter parameter, HttpServletRequest request) throws Exception {
        Cookie cookieValue = WebUtils.getCookie(request, name);
        if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
            return cookieValue;
        } else if (cookieValue != null) {
            return this.urlPathHelper.decodeRequestString(request, cookieValue.getValue());
        } else {
            return null;
        }
    }
}