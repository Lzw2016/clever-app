//package org.clever.web.exception;
//
//import org.clever.core.MethodParameter;
//import org.clever.web.exception.ServletRequestBindingException;
//
///**
// * {@link ServletRequestBindingException} subclass that indicates
// * that a request cookie expected in the method parameters of an
// * {@code @RequestMapping} method is not present.
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 22:59 <br/>
// *
// * @see MissingRequestHeaderException
// */
//public class MissingRequestCookieException extends MissingRequestValueException {
//
//    private final String cookieName;
//
//    private final MethodParameter parameter;
//
//
//    /**
//     * Constructor for MissingRequestCookieException.
//     *
//     * @param cookieName the name of the missing request cookie
//     * @param parameter  the method parameter
//     */
//    public MissingRequestCookieException(String cookieName, MethodParameter parameter) {
//        this(cookieName, parameter, false);
//    }
//
//    /**
//     * Constructor for use when a value was present but converted to {@code null}.
//     *
//     * @param cookieName             the name of the missing request cookie
//     * @param parameter              the method parameter
//     * @param missingAfterConversion whether the value became null after conversion
//     * @since 5.3.6
//     */
//    public MissingRequestCookieException(
//            String cookieName, MethodParameter parameter, boolean missingAfterConversion) {
//
//        super("", missingAfterConversion);
//        this.cookieName = cookieName;
//        this.parameter = parameter;
//    }
//
//
//    @Override
//    public String getMessage() {
//        return "Required cookie '" + this.cookieName + "' for method parameter type " +
//                this.parameter.getNestedParameterType().getSimpleName() + " is " +
//                (isMissingAfterConversion() ? "present but converted to null" : "not present");
//    }
//
//    /**
//     * Return the expected name of the request cookie.
//     */
//    public final String getCookieName() {
//        return this.cookieName;
//    }
//
//    /**
//     * Return the method parameter bound to the request cookie.
//     */
//    public final MethodParameter getParameter() {
//        return this.parameter;
//    }
//
//}
