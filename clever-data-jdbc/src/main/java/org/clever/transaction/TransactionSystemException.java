package org.clever.transaction;

import org.clever.util.Assert;

/**
 * 遇到常规事务系统错误（如提交或回滚时）时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:53 <br/>
 */
public class TransactionSystemException extends TransactionException {
    private Throwable applicationException;

    public TransactionSystemException(String msg) {
        super(msg);
    }

    public TransactionSystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * 设置在此事务异常之前引发的应用程序异常，尽管覆盖了TransactionSystemException，但仍保留原始异常。
     *
     * @param ex 应用程序异常
     * @throws IllegalStateException 如果此TransactionSystemException已包含应用程序异常
     */
    public void initApplicationException(Throwable ex) {
        Assert.notNull(ex, "Application exception must not be null");
        if (this.applicationException != null) {
            throw new IllegalStateException("Already holding an application exception: " + this.applicationException);
        }
        this.applicationException = ex;
    }

    /**
     * 返回在此事务异常之前引发的应用程序异常（如果有）。
     */
    public final Throwable getApplicationException() {
        return this.applicationException;
    }

    /**
     * 返回失败事务中第一个引发的异常：
     * 即应用程序异常（如果有）或TransactionSystemException自身的原因。
     */
    public Throwable getOriginalException() {
        return (this.applicationException != null ? this.applicationException : getCause());
    }

    @Override
    public boolean contains(Class<?> exType) {
        return super.contains(exType) || (exType != null && exType.isInstance(this.applicationException));
    }
}
