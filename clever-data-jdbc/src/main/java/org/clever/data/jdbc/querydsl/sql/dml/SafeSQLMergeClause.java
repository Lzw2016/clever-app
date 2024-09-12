package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.DefaultMapper;
import com.querydsl.sql.dml.Mapper;
import com.querydsl.sql.dml.SQLMergeClause;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Map;
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
    public <T> SQLMergeClause set(Path<T> path, @Nullable T value) {
        SQLClause.set(this, path, value);
        return this;
    }

    @Override
    public <T> SQLMergeClause set(Path<T> path, Expression<? extends T> expression) {
        SQLClause.set(this, path, expression);
        return this;
    }

    public SQLMergeClause setx(Path<?> path, Object value) {
        SQLClause.setx(this, path, value);
        return this;
    }

    public SQLMergeClause populate(Object bean) {
        return populate(bean, DefaultMapper.DEFAULT);
    }

    public <T> SQLMergeClause populate(T obj, Mapper<T> mapper) {
        SQLClause.populate(this, entity, obj, mapper);
        return this;
    }

    public SQLMergeClause populate(Map<String, ?> valueMap) {
        return populate(valueMap, MapMapper.DEFAULT);
    }

    @Override
    public long execute() {
        context = startContext(connection(), metadata, entity);
        try {
            // 参考: 父类的 execute
            long rc;
            if (configuration.getTemplates().isNativeMerge()) {
                rc = executeNativeMerge();
            } else {
                rc = executeCompositeMerge();
            }
            context.setData(SqlLoggerUtils.QUERYDSL_UPDATE_TOTAL, rc);
            return rc;
        } finally {
            reset();
            endContext(context);
        }
    }
}
