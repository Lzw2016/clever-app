package org.clever.web.support.mvc;

import org.clever.core.Ordered;

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
     * @param context 上下文
     * @return true: 继续执行下面的拦截器逻辑, false: 中断执行
     */
    default boolean beforeHandle(HandlerContext context) throws Exception {
        return true;
    }

    /**
     * 拦截器后置处理
     *
     * @param context 上下文
     */
    default void afterHandle(HandlerContext.After context) throws Exception {
    }

    /**
     * beforeHandle、handlerMethod、afterHandle 执行完成后的回调(支持后面Handler的异常处理)
     *
     * @param context 上下文
     */
    default void finallyHandle(HandlerContext.Finally context) throws Exception {
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
