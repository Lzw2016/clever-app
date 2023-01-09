package org.clever.web.exception;

/**
 * 致命绑定异常，当我们想将绑定异常视为不可恢复时抛出。
 *
 * <p>扩展 ServletException 以方便地抛出任何 Servlet 资源（例如过滤器），
 * 并扩展 NestedServletException 以进行正确的根本原因处理（因为普通的 ServletException 根本不暴露其根本原因）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:30 <br/>
 */
public class ServletRequestBindingException extends NestedServletException {
    /**
     * ServletRequestBindingException 的构造函数。
     *
     * @param msg 详细信息
     */
    public ServletRequestBindingException(String msg) {
        super(msg);
    }

    /**
     * ServletRequestBindingException 的构造函数
     *
     * @param msg   详细信息
     * @param cause 根本原因
     */
    public ServletRequestBindingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
