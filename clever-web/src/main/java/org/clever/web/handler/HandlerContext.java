package org.clever.web.handler;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * mvc请求处理上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 14:57 <br/>
 */
@Data
public class HandlerContext {
    /**
     * mvc请求处理器
     */
    private final HandleMethod handleMethod;
    /**
     * 请求 request
     */
    private final HttpServletRequest request;
    /**
     * 响应 response
     */
    private final HttpServletResponse response;
    /**
     * 处理请求程序method的参数值
     */
    private final Object[] args;

    public HandlerContext(HandleMethod handleMethod, HttpServletRequest request, HttpServletResponse response, Object[] args) {
        this.handleMethod = handleMethod;
        this.request = request;
        this.response = response;
        this.args = args;
    }
}
