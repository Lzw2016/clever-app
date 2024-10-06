package org.clever.web.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 异常处理工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 21:48 <br/>
 */
@FunctionalInterface
public interface ExceptionHandler<T extends Throwable> {
    /***
     * 处理异常
     * @param exception 服务端异常
     * @param request 请求
     * @param response 响应
     */
    void handle(T exception, HttpServletRequest request, HttpServletResponse response);
}
