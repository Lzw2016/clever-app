package org.clever.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}类使用的批更新回调接口。
 * 类使用的批更新回调接口。
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
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see InterruptibleBatchPreparedStatementSetter
 */
public interface BatchPreparedStatementSetter {
    /**
     * 在给定的PreparedStatement上设置参数值。
     *
     * @param ps 要在上调用setter方法的PreparedStatement
     * @param i  我们在批中发布的语句的索引，从0开始
     * @throws SQLException 如果遇到SQLException (即 不需要捕捉SQLException)
     */
    void setValues(PreparedStatement ps, int i) throws SQLException;

    /**
     * 返回批次大小。
     *
     * @return 批处理中的语句数
     */
    int getBatchSize();
}
