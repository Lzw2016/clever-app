package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.querydsl.sql.SQLUpdateFill;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.util.Assert;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        if (path != null
                && value != null
                && path.getType() != null
                && !path.getType().isAssignableFrom(value.getClass())) {
            return setx(path, value);
        }
        return super.set(path, value);
    }

    @Override
    public <T> SQLUpdateClause set(Path<T> path, Expression<? extends T> expression) {
        if (path != null
                && expression != null
                && path.getType() != null
                && expression.getType() != null
                && !path.getType().isAssignableFrom(expression.getType())) {
            return setx(path, expression);
        }
        return super.set(path, expression);
    }

    public SQLUpdateClause setx(Path<?> path, Object value) {
        SQLClause.setx(this, path, value);
        return this;
    }

    @Override
    public long execute() {
        if (autoFill && entity != null && entity.getColumns() != null) {
            for (SQLUpdateFill sqlUpdateFill : SQL_UPDATE_FILL_LIST) {
                sqlUpdateFill.fill(entity, metadata, updates, batches, !batches.isEmpty());
            }
        }
        return super.execute();
    }
}
