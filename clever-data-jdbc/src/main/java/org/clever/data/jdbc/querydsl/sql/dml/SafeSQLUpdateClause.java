package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.querydsl.sql.SQLUpdateFill;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.clever.util.Assert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

/**
 * 自定义SQLUpdateClause主要职责： <br/>
 * 1.处理set null值报错问题 <br/>
 * 2.新增SQLUpdateFill功能 <br/>
 * 3.新增setx函数，自动类型转换 <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 22:33 <br/>
 */
@Slf4j
public class SafeSQLUpdateClause extends SQLUpdateClause {
    protected static volatile List<SQLUpdateFill> SQL_UPDATE_FILL_LIST = new ArrayList<>();

    /**
     * 新增SQLUpdateFill
     */
    public static synchronized void addSQLUpdateFill(SQLUpdateFill sqlUpdateFill) {
        Assert.notNull(sqlUpdateFill, "参数sqlUpdateFill不能为空");
        List<SQLUpdateFill> list = new ArrayList<>(SQL_UPDATE_FILL_LIST.size() + 1);
        list.addAll(SQL_UPDATE_FILL_LIST);
        list.add(sqlUpdateFill);
        list.sort(Comparator.comparingInt(SQLUpdateFill::getOrder));
        SQL_UPDATE_FILL_LIST = list;
    }

    /**
     * 是否自动填充字段
     */
    private boolean autoFill = true;

    public SafeSQLUpdateClause(Supplier<Connection> connection, Configuration configuration, RelationalPath<?> entity) {
        super(connection, configuration, entity);
    }

    public SQLUpdateClause autoFill() {
        autoFill = true;
        return this;
    }

    public SQLUpdateClause autoFill(boolean autoFill) {
        this.autoFill = autoFill;
        return this;
    }

    @Override
    public <T> SQLUpdateClause set(Path<T> path, T value) {
        SQLClause.set(this, path, value);
        return this;
    }

    @Override
    public <T> SQLUpdateClause set(Path<T> path, Expression<? extends T> expression) {
        SQLClause.set(this, path, expression);
        return this;
    }

    public SQLUpdateClause setx(Path<?> path, Object value) {
        SQLClause.setx(this, path, value);
        return this;
    }

    public SQLUpdateClause populate(Map<String, ?> valueMap) {
        return populate(valueMap, MapMapper.DEFAULT);
    }

    @Override
    public long execute() {
        if (autoFill && entity != null && entity.getColumns() != null) {
            for (SQLUpdateFill sqlUpdateFill : SQL_UPDATE_FILL_LIST) {
                sqlUpdateFill.fill(entity, metadata, updates, batches, !batches.isEmpty());
            }
        }
        // 参考: 父类的 execute
        context = startContext(connection(), metadata, entity);
        PreparedStatement stmt = null;
        Collection<PreparedStatement> stmts = null;
        try {
            if (batches.isEmpty()) {
                stmt = createStatement();
                listeners.notifyUpdate(entity, metadata, updates);
                listeners.preExecute(context);
                int rc = stmt.executeUpdate();
                context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
                listeners.executed(context);
                return rc;
            } else {
                stmts = createStatements();
                listeners.notifyUpdates(entity, batches);
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
