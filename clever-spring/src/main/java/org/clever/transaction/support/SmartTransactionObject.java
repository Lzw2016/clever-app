package org.clever.transaction.support;

import java.io.Flushable;

/**
 * 由能够返回内部仅回滚标记的事务对象实现的接口，通常来自已参与并将其标记为仅回滚的另一个事务。
 * <p>由DefaultTransactionStatus自动检测，即使不是由当前TransactionStatus生成的，也始终返回当前rollbackOnly标志。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 20:30 <br/>
 *
 * @see DefaultTransactionStatus#isRollbackOnly
 */
public interface SmartTransactionObject extends Flushable {
    /**
     * 返回事务是否在内部标记为仅回滚。例如，可以检查JTA UserTransaction
     */
    boolean isRollbackOnly();

    /**
     * 如果适用，将底层会话刷新到数据存储：例如，所有受影响的Hibernate/JPA会话
     */
    @Override
    void flush();
}
