package org.clever.jdbc.datasource;

import java.sql.Connection;

/**
 * 由连接代理实现的{@link Connection}的子接口。允许访问基础目标连接。
 * 当需要转换到本机JDBC连接（如Oracle的OracleConnection）时，可以检查此接口。
 * 或者，所有此类连接也支持JDBC 4.0的{@link Connection#unwrap}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:46 <br/>
 *
 * @see TransactionAwareDataSourceProxy
 * @see DataSourceUtils#getTargetConnection(Connection)
 */
public interface ConnectionProxy extends Connection {
    /**
     * 返回此代理的目标连接。
     * <p>这通常是本机驱动程序连接或连接池中的包装。
     *
     * @return 基础连接 (从不为 {@code null})
     */
    Connection getTargetConnection();
}
