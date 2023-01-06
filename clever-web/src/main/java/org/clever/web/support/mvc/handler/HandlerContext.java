package org.clever.web.support.mvc.handler;

import lombok.Data;
import org.clever.web.support.mvc.HandlerMethod;

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
     * 请求 request
     */
    private final HttpServletRequest request;
    /**
     * 响应 response
     */
    private final HttpServletResponse response;
    /**
     * mvc请求处理器
     */
    private final HandlerMethod handleMethod;
    /**
     * 处理请求程序method的参数值
     */
    private final Object[] args;

    public HandlerContext(HttpServletRequest request, HttpServletResponse response, HandlerMethod handleMethod, Object[] args) {
        this.request = request;
        this.response = response;
        this.handleMethod = handleMethod;
        this.args = args;
    }
}
