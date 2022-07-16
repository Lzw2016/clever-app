package org.clever.jdbc.datasource;

import org.clever.util.Assert;

import java.sql.Connection;

/**
 * {@link ConnectionHandle}接口的简单实现，包含给定的JDBC连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:57 <br/>
 */
public class SimpleConnectionHandle implements ConnectionHandle {
    private final Connection connection;

    public SimpleConnectionHandle(Connection connection) {
        Assert.notNull(connection, "Connection must not be null");
        this.connection = connection;
    }

    /**
     * 按原样返回指定的连接
     */
    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public String toString() {
        return "SimpleConnectionHandle: " + this.connection;
    }
}
