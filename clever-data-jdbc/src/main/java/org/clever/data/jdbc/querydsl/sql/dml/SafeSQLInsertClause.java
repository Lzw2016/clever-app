package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLInsertClause;
import org.clever.data.jdbc.querydsl.sql.SQLInsertFill;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.util.Assert;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
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
        if (path != null
                && value != null
                && path.getType() != null
                && !path.getType().isAssignableFrom(value.getClass())) {
            return setx(path, value);
        }
        return super.set(path, value);
    }

    @Override
    public <T> SQLInsertClause set(Path<T> path, Expression<? extends T> expression) {
        if (path != null
                && expression != null
                && path.getType() != null
                && expression.getType() != null
                && !path.getType().isAssignableFrom(expression.getType())) {
            return setx(path, expression);
        }
        if (expression == null) {
            return super.setNull(path);
        } else {
            return super.set(path, expression);
        }
    }

    public SQLInsertClause setx(Path<?> path, Object value) {
        SQLClause.setx(this, path, value);
        return this;
    }

    @Override
    public long execute() {
        if (autoFill && entity != null && entity.getColumns() != null) {
            for (SQLInsertFill sqlInsertFill : SQL_INSERT_FILL_LIST) {
                sqlInsertFill.fill(entity, metadata, columns, values, batches, !batches.isEmpty());
            }
        }
        return super.execute();
    }
}
