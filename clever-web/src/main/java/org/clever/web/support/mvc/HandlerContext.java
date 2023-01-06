package org.clever.web.support.mvc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
     * Handler Method
     */
    private final HandlerMethod handleMethod;
    /**
     * Handler Method的参数值
     */
    private final Object[] args;

    public HandlerContext(HttpServletRequest request, HttpServletResponse response, HandlerMethod handleMethod, Object[] args) {
        this.request = request;
        this.response = response;
        this.handleMethod = handleMethod;
        this.args = args;
    }

    @Setter
    @Getter
    public static class After extends HandlerContext {
        /**
         * Handler Method的返回值(可以覆盖返回值)
         */
        private Object result;

        public After(HttpServletRequest request, HttpServletResponse response, HandlerMethod handleMethod, Object[] args, Object result) {
            super(request, response, handleMethod, args);
            this.result = result;
        }

        public After(HandlerContext context, Object result) {
            // Assert.notNull(context, "参数 context 不能为 null");
            super(context.request, context.response, context.handleMethod, context.args);
            this.result = result;
        }
    }

    @Setter
    @Getter
    public static class Finally extends HandlerContext {
        /**
         * mvc的返回值
         */
        private final Object result;
        /**
         * 执行 beforeHandle、handlerMethod、afterHandle 时的异常对象
         */
        private final Throwable exception;

        public Finally(HttpServletRequest request, HttpServletResponse response, HandlerMethod handleMethod, Object[] args, Object result, Throwable exception) {
            super(request, response, handleMethod, args);
            this.result = result;
            this.exception = exception;
        }

        public Finally(HandlerContext context, Object result, Throwable exception) {
            // Assert.notNull(context, "参数 context 不能为 null");
            super(context.request, context.response, context.handleMethod, context.args);
            this.result = result;
            this.exception = exception;
        }
    }
}
