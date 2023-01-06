package org.clever.web.support.mvc.handler;

import org.clever.core.Ordered;
import org.clever.web.support.mvc.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理当前请求解析 HandlerContext
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:45 <br/>
 */
public interface HandlerContextArgsResolver extends Ordered {
    /**
     * 获取HandleMethod的参数
     *
     * @param request      请求对象
     * @param response     响应对象
     * @param handleMethod HandleMethod
     */
    HandlerContext getHandlerContext(HttpServletRequest request, HttpServletResponse response, HandlerMethod handleMethod);

    @Override
    default double getOrder() {
        return 0;
    }
}
