package org.clever.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}类使用的通用回调接口
 *
 * <p>此接口在JdbcTemplate类提供的{@link java.sql.PreparedStatement}上为使用相同SQL的批处理中的每个更新设置值。
 * 实现负责设置任何必要的参数。已提供带有占位符的SQL。
 *
 * <p>使用这个接口比{@link PreparedStatementCreator}更容易：
 * JdbcTemplate将创建PreparedStatement，回调只负责设置参数值。
 *
 * <p>实现不需要关心它们尝试的操作可能引发的SQLException。
 * JdbcTemplate类将捕获并适当处理SQLException。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:32 <br/>
 *
 * @see JdbcTemplate#update(String, PreparedStatementSetter)
 * @see JdbcTemplate#query(String, PreparedStatementSetter, ResultSetExtractor)
 */
@FunctionalInterface
public interface PreparedStatementSetter {
    /**
     * 在给定的PreparedStatement上设置参数值。
     *
     * @param ps 要在上调用setter方法的PreparedStatement
     * @throws SQLException 如果遇到SQLException（即不需要捕捉SQLException）
     */
    void setValues(PreparedStatement ps) throws SQLException;
}
