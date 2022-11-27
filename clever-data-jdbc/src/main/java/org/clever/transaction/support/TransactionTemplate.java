package org.clever.transaction.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.clever.transaction.*;
import org.clever.util.Assert;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * 简化编程事务划分和事务异常处理的模板类。
 *
 * <p>核心方法是 {@link #execute}，支持实现 {@link TransactionCallback} 接口的事务代码。
 * 此模板处理事务生命周期和可能的异常，因此 TransactionCallback 实现和调用代码都不需要显式处理事务。
 *
 * <p>典型用法：允许编写使用 JDBC 数据源等资源但本身不支持事务的低级数据访问对象。
 * 相反，它们可以隐式参与由使用此类的更高级别应用程序服务处理的事务，通过内部类回调对象调用低级别服务。
 *
 * <p>可以通过使用事务管理器引用直接实例化在服务实现中使用，或者在应用程序上下文中准备好并作为 bean 引用传递给服务。
 * 注意：事务管理器应始终在应用程序上下文中配置为 bean：在第一种情况下直接提供给服务，在第二种情况下提供给准备好的模板。
 *
 * <p>支持通过名称设置传播行为和隔离级别，方便在上下文定义中配置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 22:10 <br/>
 *
 * @see #execute
 * @see #setTransactionManager
 * @see org.springframework.transaction.PlatformTransactionManager
 */
public class TransactionTemplate extends DefaultTransactionDefinition implements TransactionOperations {
    protected final Log logger = LogFactory.getLog(getClass());

    private PlatformTransactionManager transactionManager;

    /**
     * 为 bean 使用构建一个新的 TransactionTemplate。
     * <p>注意：需要在任何 {@code execute} 调用之前设置 PlatformTransactionManager
     *
     * @see #setTransactionManager
     */
    public TransactionTemplate() {
    }

    /**
     * 使用给定的事务管理器构造一个新的 TransactionTemplate
     *
     * @param transactionManager 要使用的事务管理策略
     */
    public TransactionTemplate(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 使用给定的事务管理器构造一个新的 TransactionTemplate，从给定的事务定义中获取其默认设置。
     *
     * @param transactionManager    要使用的事务管理策略
     * @param transactionDefinition 要从中复制默认设置的事务定义。本地属性仍然可以设置为更改值。
     */
    public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        super(transactionDefinition);
        this.transactionManager = transactionManager;
    }

    /**
     * 设置要使用的事务管理策略。
     */
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 返回要使用的事务管理策略。
     */
    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
            return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
        } else {
            TransactionStatus status = this.transactionManager.getTransaction(this);
            T result;
            try {
                result = action.doInTransaction(status);
            } catch (RuntimeException | Error ex) {
                // Transactional code threw application exception -> rollback
                rollbackOnException(status, ex);
                throw ex;
            } catch (Throwable ex) {
                // Transactional code threw unexpected exception -> rollback
                rollbackOnException(status, ex);
                throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
            }
            this.transactionManager.commit(status);
            return result;
        }
    }

    /**
     * 执行回滚，正确处理回滚异常。
     *
     * @param status 代表事务的对象
     * @param ex     抛出的应用程序异常或错误
     * @throws TransactionException 如果发生回滚错误
     */
    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        logger.debug("Initiating transaction rollback on application exception", ex);
        try {
            this.transactionManager.rollback(status);
        } catch (TransactionSystemException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initApplicationException(ex);
            throw ex2;
        } catch (RuntimeException | Error ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate) || getTransactionManager() == ((TransactionTemplate) other).getTransactionManager())));
    }
}
