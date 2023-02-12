package org.clever.transaction;

/**
 * 指定无效超时时引发的异常，即指定的有效超时超出范围或事务管理器实现不支持超时。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:24 <br/>
 */
public class InvalidTimeoutException extends TransactionUsageException {
    private final int timeout;

    /**
     * InvalidTimeoutException的构造函数。
     *
     * @param msg     详细信息
     * @param timeout 无效的超时值
     */
    public InvalidTimeoutException(String msg, int timeout) {
        super(msg);
        this.timeout = timeout;
    }

    /**
     * 返回无效的超时值。
     */
    public int getTimeout() {
        return this.timeout;
    }
}
