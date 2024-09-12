package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLDeleteClause;
import org.clever.data.jdbc.support.SqlLoggerUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 自定义SQLDeleteClause主要职责： <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 22:34 <br/>
 */
public class SafeSQLDeleteClause extends SQLDeleteClause {
    public SafeSQLDeleteClause(Supplier<Connection> connection, Configuration configuration, RelationalPath<?> entity) {
        super(connection, configuration, entity);
    }

    @Override
    public long execute() {
        // 参考: 父类的 execute
        context = startContext(connection(), metadata, entity);
        PreparedStatement stmt = null;
        Collection<PreparedStatement> stmts = null;
        try {
            if (batches.isEmpty()) {
                stmt = createStatement();
                listeners.notifyDelete(entity, metadata);
                listeners.preExecute(context);
                int rc = stmt.executeUpdate();
                context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
                listeners.executed(context);
                return rc;
            } else {
                stmts = createStatements();
                listeners.notifyDeletes(entity, batches);
                listeners.preExecute(context);
                long rc = executeBatch(stmts);
                context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
                listeners.executed(context);
                return rc;
            }
        } catch (SQLException e) {
            onException(context, e);
            throw configuration.translate(queryString, constants, e);
        } finally {
            if (stmt != null) {
                close(stmt);
            }
            if (stmts != null) {
                close(stmts);
            }
            reset();
            endContext(context);
        }
    }
}
