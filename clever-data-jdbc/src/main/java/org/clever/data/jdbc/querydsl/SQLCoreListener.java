package org.clever.data.jdbc.querydsl;

import com.querydsl.sql.SQLBaseListener;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLListenerContext;
import lombok.Getter;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.listener.JdbcListeners;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.clever.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.clever.jdbc.datasource.DataSourceUtils;
import org.clever.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 核心SQL监听器<br/>
 * 1.获取数据库连接，归还数据库连接<p>
 * 2.执行SQL之前的操作拦截<p>
 * 3.打印执行SQL日志<p>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/14 09:43 <br/>
 */
public class SQLCoreListener extends SQLBaseListener {
    private final DbType dbType;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcListeners listeners;
    @Getter
    private final Supplier<Connection> connProvider;
    private final DataSource dataSource;

    public SQLCoreListener(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate, JdbcListeners listeners) {
        Assert.notNull(dbType, "参数dbType不能为空");
        Assert.notNull(jdbcTemplate, "参数jdbcTemplate不能为空");
        Assert.notNull(jdbcTemplate.getJdbcTemplate().getDataSource(), "参数jdbcTemplate.getDataSource()不能为空");
        Assert.notNull(listeners, "参数listeners不能为空");
        this.dbType = dbType;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = jdbcTemplate.getJdbcTemplate().getDataSource();
        this.listeners = listeners;
        this.connProvider = () -> DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public void prePrepare(SQLListenerContext context) {
        listeners.beforeExec(dbType, jdbcTemplate);
    }

    @Override
    public void preExecute(SQLListenerContext context) {
        // 打印SQL see com.querydsl.sql.AbstractSQLQuery.logQuery
        Collection<SQLBindings> allSqlBindings = context.getAllSQLBindings();
        if (allSqlBindings != null && !allSqlBindings.isEmpty()) {
            for (SQLBindings sqlBindings : allSqlBindings) {
                SqlLoggerUtils.printfSql(sqlBindings.getSQL(), sqlBindings.getNullFriendlyBindings());
            }
        }
    }

    @Override
    public void executed(SQLListenerContext context) {
    }

    @Override
    public void end(SQLListenerContext context) {
        try {
            listeners.afterExec(dbType, jdbcTemplate, context.getException());
        } finally {
            Connection connection = context.getConnection();
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }
}
