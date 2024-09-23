//package org.clever.web.support.mvc.argument;
//
//import org.clever.core.MethodParameter;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.util.Collection;
//import java.util.Optional;
//
///**
// * 用于在给定请求的上下文中将方法参数解析为参数值的策略接口
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/01 21:10 <br/>
// */
//public interface HandlerMethodArgumentResolver {
//    /**
//     * 此解析器是否支持给定的 {@linkplain MethodParameter 方法参数}
//     *
//     * @param parameter 要检查的方法参数
//     * @return {@code true} 如果解析器支持提供的参数； {@code false} 否则
//     */
//    boolean supportsParameter(MethodParameter parameter, HttpServletRequest request);
//
//    /**
//     * 将方法参数解析为给定请求的参数值
//     *
//     * @param parameter 要解析的方法参数
//     * @param request   当前请求
//     * @param response  当前响应
//     * @return 已解析的参数值，如果不可解析，则为 {@code null}
//     * @throws Exception 如果参数值的准备出错
//     */
//    Object resolveArgument(MethodParameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception;
//
//    /**
//     * 处理 Optional 类型参数
//     */
//    static Object adaptArgumentIfNecessary(Object arg, MethodParameter parameter) {
//        if (parameter.getParameterType() == Optional.class) {
//            if (arg == null
//                    || (arg instanceof Collection && ((Collection<?>) arg).isEmpty())
//                    || (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
//                return Optional.empty();
//            } else {
//                return Optional.of(arg);
//            }
//        }
//        return arg;
//    }
//}
