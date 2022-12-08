package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLMergeClause;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * 自定义SQLMergeClause主要职责： <br/>
 * 1.处理insert set null值报错问题 <br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 22:34 <br/>
 */
public class SafeSQLMergeClause extends SQLMergeClause {
    public SafeSQLMergeClause(Supplier<Connection> connection, Configuration configuration, RelationalPath<?> entity) {
        super(connection, configuration, entity);
    }

    @Override
    public <T> SQLMergeClause set(Path<T> path, Expression<? extends T> expression) {
        if (expression == null) {
            return super.setNull(path);
        } else {
            return super.set(path, expression);
        }
    }
}
