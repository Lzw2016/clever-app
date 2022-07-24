package org.clever.web.handler;

import org.clever.core.Ordered;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理当前请求解析 HandleMethod
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:45 <br/>
 */
public interface HandleMethodResolver extends Ordered {
    /**
     * 返回HandlerContext，不存在返回null
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param matcherPath 匹配的url path
     */
    HandleMethod getHandleMethod(HttpServletRequest request, HttpServletResponse response, String matcherPath);

    @Override
    default int getOrder() {
        return 0;
    }
}
