//package org.clever.web.support.mvc.interceptor;
//
//import org.clever.core.Ordered;
//import org.clever.web.support.mvc.HandlerContext;
//
///**
// * mvc拦截器
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/07/24 15:28 <br/>
// */
//public interface HandlerInterceptor extends Ordered {
//    /**
//     * Handler Method 执行之前回调<br/>
//     * 当前函数异常之后会中断Interceptor调用链
//     *
//     * @param context 上下文
//     * @return true: 继续执行下面的拦截器逻辑, false: 中断执行
//     */
//    default boolean beforeHandle(HandlerContext context) throws Exception {
//        return true;
//    }
//
//    /**
//     * Handler Method 执行之后回调(支持更新 Handler Method 的返回值)<br/>
//     * 当前函数异常之后会中断Interceptor调用链
//     *
//     * @param context 上下文
//     */
//    default void afterHandle(HandlerContext.After context) throws Exception {
//    }
//
//    /**
//     * beforeHandle、handlerMethod、afterHandle 执行完成后的回调(支持后面Handler的异常处理)<br/>
//     * 执行当前 beforeHandle 后<b>一定会执行</b>此函数，当前函数生产的异常<b>不会中断</b>Interceptor调用链
//     *
//     * @param context 上下文
//     */
//    default void finallyHandle(HandlerContext.Finally context) throws Exception {
//    }
//
//    /**
//     * 返回拦截器执行顺序
//     *
//     * @see org.clever.core.Ordered
//     */
//    @Override
//    default double getOrder() {
//        return 0;
//    }
//}
