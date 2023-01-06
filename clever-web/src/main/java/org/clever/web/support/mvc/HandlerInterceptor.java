package org.clever.web.support.mvc;

import org.clever.core.Ordered;

/**
 * mvc拦截器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:28 <br/>
 */
public interface HandlerInterceptor extends Ordered {
    /**
     * Handler Method 执行之前回调<br/>
     * 异常之后会中断Interceptor链
     *
     * @param context 上下文
     * @return true: 继续执行下面的拦截器逻辑, false: 中断执行
     */
    default boolean beforeHandle(HandlerContext context) throws Exception {
        return true;
    }

    /**
     * Handler Method 执行之后回调(支持更新 Handler Method 的返回值)<br/>
     * 异常之后会中断Interceptor链
     *
     * @param context 上下文
     */
    default void afterHandle(HandlerContext.After context) throws Exception {
    }

    /**
     * beforeHandle、handlerMethod、afterHandle 执行完成后的回调(支持后面Handler的异常处理)<br/>
     * 执行当前 beforeHandle 后一定会执行的回调，当前函数生产的异常会被忽略
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