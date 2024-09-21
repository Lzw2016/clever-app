package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLInsertClause;
import org.clever.core.Assert;
import org.clever.data.jdbc.querydsl.sql.SQLInsertFill;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

/**
 * 自定义SQLInsertClause主要职责： <br/>
 * 1.处理set null值报错问题 <br/>
 * 2.新增SQLInsertFill功能 <br/>
 * 3.新增setx函数，自动类型转换 <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 18:03 <br/>
 */
public class SafeSQLInsertClause extends SQLInsertClause {
    protected static volatile List<SQLInsertFill> SQL_INSERT_FILL_LIST = new ArrayList<>();

    /**
     * 新增SQLInsertFill
     */
    public static synchronized void addSQLInsertFill(SQLInsertFill sqlInsertFill) {
        Assert.notNull(sqlInsertFill, "参数sqlInsertFill不能为空");
        List<SQLInsertFill> list = new ArrayList<>(SQL_INSERT_FILL_LIST.size() + 1);
        list.addAll(SQL_INSERT_FILL_LIST);
        list.add(sqlInsertFill);
        list.sort(Comparator.comparingInt(SQLInsertFill::getOrder));
        SQL_INSERT_FILL_LIST = list;
    }

    /**
     * 是否自动填充字段
     */
    private boolean autoFill = true;
    /**
     * 原始的 set 函数
     */
    private final SQLClause.StoreClauseRawSet rawSet = new SQLClause.StoreClauseRawSet() {
        @Override
        public <T> void set(Path<T> path, @Nullable T value) {
            SafeSQLInsertClause.super.set(path, value);
        }

        @Override
        public <T> void set(Path<T> path, Expression<? extends T> expression) {
            SafeSQLInsertClause.super.set(path, expression);
        }

        @Override
        public <T> void setNull(Path<T> path) {
            SafeSQLInsertClause.super.setNull(path);
        }
    };

    public SafeSQLInsertClause(Supplier<Connection> connection, Configuration configuration, RelationalPath<?> entity) {
        super(connection, configuration, entity);
    }

    public SQLInsertClause autoFill() {
        autoFill = true;
        return this;
    }

    public SQLInsertClause autoFill(boolean autoFill) {
        this.autoFill = autoFill;
        return this;
    }

    @Override
    public <T> SQLInsertClause set(Path<T> path, T value) {
        SQLClause.set(rawSet, path, value);
        return this;
    }

    @Override
    public <T> SQLInsertClause set(Path<T> path, Expression<? extends T> expression) {
        SQLClause.set(rawSet, path, expression);
        return this;
    }

    public SQLInsertClause setx(Path<?> path, Object value) {
        SQLClause.setx(rawSet, path, value);
        return this;
    }

    public SQLInsertClause populate(Map<String, ?> valueMap) {
        return populate(valueMap, MapMapper.DEFAULT);
    }

    @Override
    public long execute() {
        if (autoFill && entity != null && entity.getColumns() != null) {
            for (SQLInsertFill sqlInsertFill : SQL_INSERT_FILL_LIST) {
                sqlInsertFill.fill(entity, metadata, columns, values, batches, !batches.isEmpty());
            }
        }
        // 参考: 父类的 execute
        context = startContext(connection(), metadata, entity);
        PreparedStatement stmt = null;
        Collection<PreparedStatement> stmts = null;
        try {
            if (batches.isEmpty()) {
                stmt = createStatement(false);
                listeners.notifyInsert(entity, metadata, columns, values, subQuery);
                listeners.preExecute(context);
                int rc = stmt.executeUpdate();
                context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
                listeners.executed(context);
                return rc;
            } else if (batchToBulk) {
                stmt = createStatement(false);
                listeners.notifyInserts(entity, metadata, batches);
                listeners.preExecute(context);
                int rc = stmt.executeUpdate();
                context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
                listeners.executed(context);
                return rc;
            } else {
                stmts = createStatements(false);
                listeners.notifyInserts(entity, metadata, batches);
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
