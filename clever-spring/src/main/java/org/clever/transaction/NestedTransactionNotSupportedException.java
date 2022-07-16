package org.clever.transaction;

/**
 * 尝试使用嵌套事务时引发异常，但基础后端不支持嵌套事务。
 *
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:28 <br/>
 */
public class NestedTransactionNotSupportedException extends CannotCreateTransactionException {
    public NestedTransactionNotSupportedException(String msg) {
        super(msg);
    }

    public NestedTransactionNotSupportedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
