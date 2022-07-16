package org.clever.jdbc.datasource;

import org.clever.util.Assert;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * JDBC {@link javax.sql.DataSource}实现，将所有调用委托给给定的目标{@link javax.sql.DataSource}。
 * <p>这个类应该是子类化的，子类只覆盖那些不应该简单地委托给目标数据源的方法（例如{@link #getConnection()}）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:44 <br/>
 *
 * @see #getConnection
 */
public class DelegatingDataSource implements DataSource {
    private DataSource targetDataSource;

    /**
     * @see #setTargetDataSource
     */
    public DelegatingDataSource() {
    }

    /**
     * @param targetDataSource 目标数据源
     */
    public DelegatingDataSource(DataSource targetDataSource) {
        setTargetDataSource(targetDataSource);
    }

    /**
     * 设置此数据源应委托给的目标数据源
     */
    public void setTargetDataSource(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    /**
     * 返回此数据源应委托给的目标数据源
     */
    public DataSource getTargetDataSource() {
        return this.targetDataSource;
    }

    /**
     * 获取实际使用的目标{@code DataSource}（从不为null）。
     */
    protected DataSource obtainTargetDataSource() {
        DataSource dataSource = getTargetDataSource();
        Assert.state(dataSource != null, "No 'targetDataSource' set");
        return dataSource;
    }

    public void afterPropertiesSet() {
        if (getTargetDataSource() == null) {
            throw new IllegalArgumentException("Property 'targetDataSource' is required");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return obtainTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return obtainTargetDataSource().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return obtainTargetDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        obtainTargetDataSource().setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return obtainTargetDataSource().getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        obtainTargetDataSource().setLoginTimeout(seconds);
    }

    //---------------------------------------------------------------------
    // Implementation of JDBC 4.0's Wrapper interface
    //---------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return obtainTargetDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) || obtainTargetDataSource().isWrapperFor(iface));
    }

    //---------------------------------------------------------------------
    // Implementation of JDBC 4.1's getParentLogger method
    //---------------------------------------------------------------------

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
}
