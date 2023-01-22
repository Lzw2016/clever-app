package org.clever.web.support.mvc.annotation;

import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.annotation.Isolation;
import org.clever.transaction.annotation.Propagation;

import java.lang.annotation.*;

/**
 * 定义当前 HandlerMethod 事务配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/20 23:03 <br/>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {
    /**
     * 禁用事务(不开启任何数据源的事务)
     */
    boolean disabled() default false;

    /**
     * 要启用事务的数据源,默认使用 {@code "jdbc.defaultName"} 配置的数据源
     */
    String[] datasource() default {};

    /**
     * 事务传播类型。
     * <p>默认为 {@link Propagation#REQUIRED}.
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * 事务隔离级别。
     *
     * @see org.clever.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * 此事务的超时（以秒为单位）。
     *
     * @return 以秒为单位的超时
     */
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

    /**
     * 如果事务实际上是只读的，则可以设置为 {@code true} 的布尔标志，允许在运行时进行相应的优化。
     *
     * @see org.clever.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
     */
    boolean readOnly() default false;
}
