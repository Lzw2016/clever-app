//package org.clever.web.support.bind;
//
//import org.clever.beans.MutablePropertyValues;
//import org.clever.web.utils.WebUtils;
//
//import javax.servlet.ServletRequest;
//
///**
// * 从 ServletRequest 中的参数创建的 PropertyValues 实现。可以查找以特定前缀和前缀分隔符（默认为“_”）开头的所有属性值。
// * <p>例如，前缀为“spring”、“spring_param1”和“spring_param2”会导致以“param1”和“param2”作为键的映射。
// * <p>此类不是不可变的，以便能够有效地删除绑定时应忽略的属性值。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/06/08 21:59 <br/>
// *
// * @see org.clever.web.utils.WebUtils#getParametersStartingWith
// */
//public class ServletRequestParameterPropertyValues extends MutablePropertyValues {
//    /**
//     * 默认前缀分隔符。
//     */
//    public static final String DEFAULT_PREFIX_SEPARATOR = "_";
//
//    /**
//     * 使用无前缀（因此，无前缀分隔符）创建新的 ServletRequestPropertyValues。
//     *
//     * @param request HTTP 请求
//     */
//    public ServletRequestParameterPropertyValues(ServletRequest request) {
//        this(request, null, null);
//    }
//
//    /**
//     * 使用给定前缀和默认前缀分隔符（下划线字符“_”）创建新的 ServletRequestPropertyValues。
//     *
//     * @param request HTTP 请求
//     * @param prefix  参数的前缀（完整的前缀将由这个加上分隔符组成）
//     * @see #DEFAULT_PREFIX_SEPARATOR
//     */
//    public ServletRequestParameterPropertyValues(ServletRequest request, String prefix) {
//        this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
//    }
//
//    /**
//     * 创建提供前缀和前缀分隔符的新 ServletRequestPropertyValues。
//     *
//     * @param request         HTTP 请求
//     * @param prefix          参数的前缀（完整的前缀将由这个加上分隔符组成）
//     * @param prefixSeparator 分隔符定界前缀（例如“clever”）和参数名称的其余部分（“param1”、“param2”）
//     */
//    public ServletRequestParameterPropertyValues(ServletRequest request, String prefix, String prefixSeparator) {
//        super(WebUtils.getParametersStartingWith(request, (prefix != null ? prefix + prefixSeparator : null)));
//    }
//}
