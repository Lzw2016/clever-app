//package org.clever.web.exception;
//
///**
// * {@link ServletRequestBindingException} 子类，表示缺少参数。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/10 21:49 <br/>
// */
//public class MissingServletRequestParameterException extends MissingRequestValueException {
//    private final String parameterName;
//    private final String parameterType;
//
//    /**
//     * MissingServletRequestParameterException 的构造函数。
//     *
//     * @param parameterName 缺少参数的名称
//     * @param parameterType 缺失参数的预期类型
//     */
//    public MissingServletRequestParameterException(String parameterName, String parameterType) {
//        this(parameterName, parameterType, false);
//    }
//
//    /**
//     * 当值存在但转换为 {@code null} 时使用的构造函数。
//     *
//     * @param parameterName          缺少参数的名称
//     * @param parameterType          缺失参数的预期类型
//     * @param missingAfterConversion 转换后值是否变为null
//     */
//    public MissingServletRequestParameterException(String parameterName, String parameterType, boolean missingAfterConversion) {
//        super("", missingAfterConversion);
//        this.parameterName = parameterName;
//        this.parameterType = parameterType;
//    }
//
//    @Override
//    public String getMessage() {
//        return "Required request parameter '" + this.parameterName
//                + "' for method parameter type " + this.parameterType
//                + " is " + (isMissingAfterConversion() ? "present but converted to null" : "not present");
//    }
//
//    /**
//     * 返回有问题的参数的名称。
//     */
//    public final String getParameterName() {
//        return this.parameterName;
//    }
//
//    /**
//     * 返回违规参数的预期类型。
//     */
//    public final String getParameterType() {
//        return this.parameterType;
//    }
//}
