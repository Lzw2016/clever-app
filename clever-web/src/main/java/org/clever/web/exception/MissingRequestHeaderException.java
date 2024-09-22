//package org.clever.web.exception;
//
//import org.clever.core.MethodParameter;
//
///**
// * {@link ServletRequestBindingException} 子类，指示不存在 {@code @RequestMapping} 方法的方法参数中预期的请求标头
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 23:02 <br/>
// *
// * @see MissingRequestCookieException
// */
//public class MissingRequestHeaderException extends MissingRequestValueException {
//    private final String headerName;
//    private final MethodParameter parameter;
//
//    /**
//     * MissingRequestHeaderException 的构造函数。
//     *
//     * @param headerName 缺少的请求标头的名称
//     * @param parameter  方法参数
//     */
//    public MissingRequestHeaderException(String headerName, MethodParameter parameter) {
//        this(headerName, parameter, false);
//    }
//
//    /**
//     * 当值存在但转换为 {@code null} 时使用的构造函数。
//     *
//     * @param headerName             缺少的请求标头的名称
//     * @param parameter              方法参数
//     * @param missingAfterConversion 转换后值是否变为null
//     */
//    public MissingRequestHeaderException(String headerName, MethodParameter parameter, boolean missingAfterConversion) {
//        super("", missingAfterConversion);
//        this.headerName = headerName;
//        this.parameter = parameter;
//    }
//
//    @Override
//    public String getMessage() {
//        String typeName = this.parameter.getNestedParameterType().getSimpleName();
//        return "Required request header '" + this.headerName + "' for method parameter type " + typeName + " is " + (isMissingAfterConversion() ? "present but converted to null" : "not present");
//    }
//
//    /**
//     * 返回请求标头的预期名称
//     */
//    public final String getHeaderName() {
//        return this.headerName;
//    }
//
//    /**
//     * 返回绑定到请求头的方法参数
//     */
//    public final MethodParameter getParameter() {
//        return this.parameter;
//    }
//}
