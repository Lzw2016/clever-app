package org.clever.dao.support;

import org.clever.dao.DataAccessException;

/**
 * 引发运行时异常的数据访问技术（如JPA和Hibernate）集成实现的接口。
 * <p>这允许一致地使用组合异常转换功能，而无需强制单个转换器理解每一种可能的异常类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:19 <br/>
 */
@FunctionalInterface
public interface PersistenceExceptionTranslator {
    /**
     * 如果可能，将持久性框架引发的给定运行时异常转换为通用{@link org.clever.dao.DataAccessException}层次结构中的相应异常。
     * <p>不要翻译此转换器无法理解的异常：例如，如果来自另一个持久性框架，或者源于用户代码，或者与持久性无关。
     * <p>尤其重要的是正确地转换为DataIntegrityViolationException，例如关于约束冲突的转换。
     * 实现可以使用JDBC复杂的异常转换，以在发生SQLException作为根本原因时提供进一步的信息。
     *
     * @param ex 要转换的运行时异常
     * @return 相应的DataAccessException(如果无法翻译异常,则为null,因为在这种情况下,它可能是由用户代码而不是实际的持久性问题导致的)
     * @see org.clever.dao.DataIntegrityViolationException
     * @see org.clever.jdbc.support.SQLExceptionTranslator
     */
    DataAccessException translateExceptionIfPossible(RuntimeException ex);
}
