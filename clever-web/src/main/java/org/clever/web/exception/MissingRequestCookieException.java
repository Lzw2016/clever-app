package org.clever.web.exception;

import org.clever.core.MethodParameter;

/**
 * {@link ServletRequestBindingException} 子类，指示 {@code @RequestMapping} 方法的方法参数中预期的请求 cookie 不存在
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:59 <br/>
 *
 * @see MissingRequestHeaderException
 */
public class MissingRequestCookieException extends MissingRequestValueException {
    private final String cookieName;
    private final MethodParameter parameter;

    /**
     * MissingRequestCookieException 的构造函数
     *
     * @param cookieName cookie 的名称
     * @param parameter  方法参数
     */
    public MissingRequestCookieException(String cookieName, MethodParameter parameter) {
        this(cookieName, parameter, false);
    }

    /**
     * 当值存在但转换为 {@code null} 时使用的构造函数
     *
     * @param cookieName             cookie 的名称
     * @param parameter              方法参数
     * @param missingAfterConversion 转换后值是否变为null
     */
    public MissingRequestCookieException(String cookieName, MethodParameter parameter, boolean missingAfterConversion) {
        super("", missingAfterConversion);
        this.cookieName = cookieName;
        this.parameter = parameter;
    }

    @Override
    public String getMessage() {
        return "Required cookie '" + this.cookieName + "' for method parameter type "
                + this.parameter.getNestedParameterType().getSimpleName() + " is "
                + (isMissingAfterConversion() ? "present but converted to null" : "not present");
    }

    /**
     * 返回请求 cookie 的预期名称
     */
    public final String getCookieName() {
        return this.cookieName;
    }

    /**
     * 返回绑定到请求 cookie 的方法参数
     */
    public final MethodParameter getParameter() {
        return this.parameter;
    }
}
