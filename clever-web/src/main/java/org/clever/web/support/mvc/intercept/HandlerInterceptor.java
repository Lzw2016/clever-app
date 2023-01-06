package org.clever.web.support.mvc.intercept;

import org.clever.core.Ordered;
import org.clever.web.support.mvc.handler.HandlerContext;

/**
 * 请求处理拦截器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:28 <br/>
 */
public interface HandlerInterceptor extends Ordered {
    /**
     * 拦截器前置处理
     *
     * @param context 处理器上下文
     * @return true: 继续执行下面的拦截器逻辑, false: 中断执行
     */
    default boolean preHandle(HandlerContext context) throws Exception {
        return true;
    }

    /**
     * 拦截器后置处理
     *
     * @param context 处理器上下文
     * @param result  Handler Method的返回值
     */
    default void postHandle(HandlerContext context, Object result) throws Exception {
    }

    /**
     * 异常时的处理(执行 preHandle、handlerMethod、postHandle 时的异常处理)
     *
     * @param context 处理器上下文
     * @param ex      异常对象
     */
    default void onException(HandlerContext context, Exception ex) throws Exception {
    }

    /**
     * 返回拦截器执行顺序
     *
     * @see org.clever.core.Ordered
     */
    @Override
    default double getOrder() {
        return 0;
    }
}
