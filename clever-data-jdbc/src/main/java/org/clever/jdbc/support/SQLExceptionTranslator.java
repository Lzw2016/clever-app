package org.clever.jdbc.support;

import org.clever.dao.DataAccessException;

import java.sql.SQLException;

/**
 * 用于在{@link SQLException}和数据访问策略不可知{@link DataAccessException}层次结构之间转换的策略接口。
 *
 * <p>实现可以是通用的(例如，使用用于JDBC的{@link SQLException#getSQLState() SQLState}代码)，
 * 也可以是完全专有的(例如，使用Oracle错误代码)，以获得更高的精度。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:42 <br/>
 *
 * @see DataAccessException
 */
@FunctionalInterface
public interface SQLExceptionTranslator {
    /**
     * 将给定的{@link SQLException}转换为通用{@link DataAccessException}
     *
     * <p>返回的DataAccessException应该包含原始{@code SQLException}作为根本原因。
     * 然而，客户端代码通常不依赖于此，因为DataAccessException可能也由其他资源API引起。
     * 也就是说，当期望发生基于JDBC的访问时，SQLException检查(和后续转换)的{@code getRootCause()}实例被认为是可靠的。
     *
     * @param task 描述正在尝试的任务的可读文本
     * @param sql  导致问题的SQL查询或更新（如果已知）
     * @param ex   {@code SQLException}
     * @return 包装{@code SQLException}的DataAccessException，如果无法应用特定转换，则为null
     * @see DataAccessException#getRootCause()
     */
    DataAccessException translate(String task, String sql, SQLException ex);
}
