package org.clever.web.support.mvc.bind.argument;

import org.clever.core.MethodParameter;
import org.clever.core.Ordered;

import javax.servlet.http.HttpServletRequest;

/**
 * 用于在给定请求的上下文中将方法参数解析为参数值的策略接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 21:10 <br/>
 */
public interface HandlerMethodArgumentResolver extends Ordered {
    /**
     * 此解析器是否支持给定的 {@linkplain MethodParameter 方法参数}
     *
     * @param parameter 要检查的方法参数
     * @return {@code true} 如果解析器支持提供的参数； {@code false} 否则
     */
    boolean supportsParameter(MethodParameter parameter, HttpServletRequest request);

    /**
     * 将方法参数解析为给定请求的参数值
     *
     * @param parameter 要解析的方法参数
     * @param request   当前请求
     * @return 已解析的参数值，如果不可解析，则为 {@code null}
     * @throws Exception 如果参数值的准备出错
     */
    Object resolveArgument(MethodParameter parameter, HttpServletRequest request) throws Exception;

    @Override
    default double getOrder() {
        return 0;
    }
}
