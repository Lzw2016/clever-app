package org.clever.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}类用于批量更新的参数化回调接口。
 *
 * <p>此接口在JdbcTemplate类提供的{@link PreparedStatement}上为使用相同SQL的批处理中的每个更新设置值。
 * 实现负责设置任何必要的参数。已提供带有占位符的SQL。
 *
 * <p>实现不需要关心它们尝试的操作可能引发的SQLException。
 * JdbcTemplate类将捕获并适当处理SQLException。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:36 <br/>
 *
 * @param <T> the argument type
 * @see JdbcTemplate#batchUpdate(String, java.util.Collection, int, ParameterizedPreparedStatementSetter)
 */
@FunctionalInterface
public interface ParameterizedPreparedStatementSetter<T> {
    /**
     * 在给定的PreparedStatement上设置参数值。
     *
     * @param ps       要在上调用setter方法的PreparedStatement
     * @param argument 包含要设置的值的对象
     * @throws SQLException 如果遇到SQLException（即不需要捕捉SQLException）
     */
    void setValues(PreparedStatement ps, T argument) throws SQLException;
}
