//package org.clever.web.exception;
//
//import org.clever.core.MethodParameter;
//import org.clever.web.exception.ServletRequestBindingException;
//
///**
// * {@link ServletRequestBindingException} subclass that indicates
// * that a request header expected in the method parameters of an
// * {@code @RequestMapping} method is not present.
// *
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 23:02 <br/>
// * @see MissingRequestCookieException
// */
//public class MissingRequestHeaderException extends MissingRequestValueException {
//
//    private final String headerName;
//
//    private final MethodParameter parameter;
//
//
//    /**
//     * Constructor for MissingRequestHeaderException.
//     * @param headerName the name of the missing request header
//     * @param parameter the method parameter
//     */
//    public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
//        this(headerName, parameter, false);
//    }
//
//    /**
//     * Constructor for use when a value was present but converted to {@code null}.
//     * @param headerName the name of the missing request header
//     * @param parameter the method parameter
//     * @param missingAfterConversion whether the value became null after conversion
//     * @since 5.3.6
//     */
//    public MissingRequestHeaderException(
//            String headerName, MethodParameter parameter, boolean missingAfterConversion) {
//
//        super("", missingAfterConversion);
//        this.headerName = headerName;
//        this.parameter = parameter;
//    }
//
//
//    @Override
//    public String getMessage() {
//        String typeName = this.parameter.getNestedParameterType().getSimpleName();
//        return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " +
//                (isMissingAfterConversion() ? "present but converted to null" : "not present");
//    }
//
//    /**
//     * Return the expected name of the request header.
//     */
//    public final String getHeaderName() {
//        return this.headerName;
//    }
//
//    /**
//     * Return the method parameter bound to the request header.
//     */
//    public final MethodParameter getParameter() {
//        return this.parameter;
//    }
//
//}
