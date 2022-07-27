package org.clever.jdbc.support;

import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * {@link org.clever.jdbc.core.JdbcTemplate}基类和其他JDBC访问DAO帮助程序，定义数据源和异常转换器等公共属性。
 *
 * <p>不打算直接使用。参考 {@link org.clever.jdbc.core.JdbcTemplate}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:39 <br/>
 *
 * @see org.clever.jdbc.core.JdbcTemplate
 */
public abstract class JdbcAccessor {
    /**
     * 布尔标志{@code clever.xml.ignore}系统属性，指示忽略XML，即不初始化与XML相关的基础结构。
     * <p>默认值为 "false".
     */
    private static final boolean shouldIgnoreXml = false;

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private DataSource dataSource;
    private volatile SQLExceptionTranslator exceptionTranslator;
    private boolean lazyInit = true;

    /**
     * 设置要从中获取连接的JDBC数据源。
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 返回此模板使用的数据源。
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * 获取数据源以供实际使用。
     *
     * @return 数据来源 (从不为 {@code null})
     * @throws IllegalStateException 如果未设置数据源
     */
    protected DataSource obtainDataSource() {
        DataSource dataSource = getDataSource();
        Assert.state(dataSource != null, "No DataSource set");
        return dataSource;
    }

    /**
     * 指定此访问者使用的数据源的数据库产品名称。
     * 这允许初始化SQLErrorCodeSQLExceptionTranslator，而无需从数据源获取连接来获取元数据。
     *
     * @param dbName 标识错误代码条目的数据库产品名称
     * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public void setDatabaseProductName(String dbName) {
        if (!shouldIgnoreXml) {
            this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
        }
    }

    /**
     * 为此实例设置异常转换器。
     * <p>如果未提供自定义转换器，则会使用默认的{@link SQLErrorCodeSQLExceptionTranslator}来检查SQLException的供应商特定错误代码。
     *
     * @see SQLErrorCodeSQLExceptionTranslator
     * @see SQLStateSQLExceptionTranslator
     */
    public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
    }

    /**
     * 返回此实例的异常转换器。
     * <p>如果未设置，则为指定的数据源创建默认的{@link SQLErrorCodeSQLExceptionTranslator}；
     * 如果未设置数据源，则为{@link SQLErrorCodeSQLExceptionTranslator}。
     *
     * @see #getDataSource()
     */
    public SQLExceptionTranslator getExceptionTranslator() {
        SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
        if (exceptionTranslator != null) {
            return exceptionTranslator;
        }
        synchronized (this) {
            exceptionTranslator = this.exceptionTranslator;
            if (exceptionTranslator == null) {
                DataSource dataSource = getDataSource();
                if (shouldIgnoreXml) {
                    exceptionTranslator = new SQLExceptionSubclassTranslator();
                } else if (dataSource != null) {
                    exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
                } else {
                    exceptionTranslator = new SQLStateSQLExceptionTranslator();
                }
                this.exceptionTranslator = exceptionTranslator;
            }
            return exceptionTranslator;
        }
    }

    /**
     * 设置是否在第一次遇到SQLException时延迟初始化此访问器的SQLExceptionTranslator。
     * 默认为“true”；启动时可以切换到“false”进行初始化。
     * <p>如果调用{@code afterPropertiesSet()}，则早期初始化仅适用。
     *
     * @see #getExceptionTranslator()
     * @see #afterPropertiesSet()
     */
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    /**
     * 返回是否延迟初始化此访问器的SQLExceptionTranslator。
     *
     * @see #getExceptionTranslator()
     */
    public boolean isLazyInit() {
        return this.lazyInit;
    }

    /**
     * 如果需要，急切地初始化异常转换器，如果未设置，则为指定的数据源创建默认的异常转换器。
     */
    public void afterPropertiesSet() {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("Property 'dataSource' is required");
        }
        if (!isLazyInit()) {
            getExceptionTranslator();
        }
    }
}
