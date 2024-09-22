//package org.clever.web.exception;
//
//import org.clever.core.NestedExceptionUtils;
//
//import javax.servlet.ServletException;
//
///**
// * {@link ServletException}的子类，它在消息和堆栈跟踪方面正确处理根本原因，
// * 就像NestedCheckedRuntimeException那样。
// * <p>注意，普通的ServletException根本没有暴露其根本原因，既不在异常消息中，也不在打印的堆栈跟踪中！
// * 虽然这可能在以后的Servlet API变体中被修复（对于相同的API版本），它在Servlet 2.4上不可靠，这就是为什么我们需要自己做。
// * <p>此类与NestedCheckedRuntimeException类之间的相似性是不可避免的，因为此类需要从ServletException派生。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 22:32 <br/>
// *
// * @see #getMessage
// * @see org.clever.core.NestedCheckedException
// * @see org.clever.core.NestedRuntimeException
// */
//public class NestedServletException extends ServletException {
//    /**
//     * 使用指定的详细信息消息构造{@code NestedServletException}
//     *
//     * @param msg the detail message
//     */
//    public NestedServletException(String msg) {
//        super(msg);
//    }
//
//    /**
//     * 使用指定的详细消息和嵌套异常构造{@code NestedServletException}
//     *
//     * @param msg   详细信息
//     * @param cause 嵌套异常
//     */
//    public NestedServletException(String msg, Throwable cause) {
//        super(msg, cause);
//    }
//
//    /**
//     * 返回详细消息，包括来自嵌套异常的消息（如果有的话）。
//     */
//    @Override
//    public String getMessage() {
//        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
//    }
//}
