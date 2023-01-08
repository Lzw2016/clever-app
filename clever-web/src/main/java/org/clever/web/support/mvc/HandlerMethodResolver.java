package org.clever.web.support.mvc;

import org.clever.web.config.MvcConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理当前请求解析 HandleMethod
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 15:45 <br/>
 */
public interface HandlerMethodResolver {
    /**
     * 获取 HandlerMethod 对象
     *
     * @param request   请求对象
     * @param response  响应对象
     * @param mvcConfig mvc配置
     */
    HandlerMethod getHandleMethod(HttpServletRequest request, HttpServletResponse response, MvcConfig mvcConfig) throws Exception;
}
