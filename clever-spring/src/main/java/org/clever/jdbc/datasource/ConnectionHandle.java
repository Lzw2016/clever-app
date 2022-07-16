package org.clever.jdbc.datasource;

import java.sql.Connection;

/**
 * 由JDBC连接的句柄实现的简单接口。例如，由JpaDialect使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:56 <br/>
 *
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 */
@FunctionalInterface
public interface ConnectionHandle {
    /**
     * 获取此句柄引用的JDBC连接
     */
    Connection getConnection();

    /**
     * 释放此句柄引用的JDBC连接。
     * <p>默认实现为空，假设连接的生命周期是在外部管理的。
     *
     * @param con the JDBC Connection to release
     */
    default void releaseConnection(Connection con) {
    }
}
