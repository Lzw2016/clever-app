package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.sql.*;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLMergeClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLDeleteClause;
import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLInsertClause;
import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLMergeClause;
import org.clever.data.jdbc.querydsl.sql.dml.SafeSQLUpdateClause;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * 基于 {@link com.querydsl.sql.AbstractSQLQueryFactory} 的改造，
 * 使用 {@link SafeSQLDeleteClause}、{@link SafeSQLInsertClause}、{@link SafeSQLMergeClause}、{@link SafeSQLUpdateClause} 替换默认的实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 22:00 <br/>
 */
public abstract class AbstractSQLQueryFactory<Q extends SQLCommonQuery<?>> implements SQLCommonQueryFactory<Q, SQLDeleteClause, SQLUpdateClause, SQLInsertClause, SQLMergeClause> {
    @Getter
    protected final Configuration configuration;
    protected final Supplier<Connection> connection;

    public AbstractSQLQueryFactory(Configuration configuration, Supplier<Connection> connProvider) {
        this.configuration = configuration;
        this.connection = connProvider;
    }

    @Override
    public SQLDeleteClause delete(RelationalPath<?> path) {
        return new SafeSQLDeleteClause(connection, configuration, path);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q from(Expression<?> from) {
        return (Q) query().from(from);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q from(Expression<?>... args) {
        return (Q) query().from(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Q from(SubQueryExpression<?> subQuery, Path<?> alias) {
        return (Q) query().from(subQuery, alias);
    }

    @Override
    public SQLInsertClause insert(RelationalPath<?> path) {
        return new SafeSQLInsertClause(connection, configuration, path);
    }

    @Override
    public SQLMergeClause merge(RelationalPath<?> path) {
        return new SafeSQLMergeClause(connection, configuration, path);
    }

    @Override
    public SQLUpdateClause update(RelationalPath<?> path) {
        return new SafeSQLUpdateClause(connection, configuration, path);
    }

    public Connection getConnection() {
        return connection.get();
    }

    /**
     * Create a new SQL query with the given projection
     *
     * @param expr projection
     * @param <T>  type of the projection
     * @return select(expr)
     */
    public abstract <T> AbstractSQLQuery<T, ?> select(Expression<T> expr);

    /**
     * Create a new SQL query with the given projection
     *
     * @param exprs projection
     * @return select(exprs)
     */
    public abstract AbstractSQLQuery<Tuple, ?> select(Expression<?>... exprs);

    /**
     * Create a new SQL query with the given projection
     *
     * @param expr distinct projection
     * @param <T>  type of the projection
     * @return select(distinct expr)
     */
    public abstract <T> AbstractSQLQuery<T, ?> selectDistinct(Expression<T> expr);

    /**
     * Create a new SQL query with the given projection
     *
     * @param exprs distinct projection
     * @return select(distinct exprs)
     */
    public abstract AbstractSQLQuery<Tuple, ?> selectDistinct(Expression<?>... exprs);

    /**
     * Create a new SQL query with zero as the projection
     *
     * @return select(0)
     */
    public abstract AbstractSQLQuery<Integer, ?> selectZero();

    /**
     * Create a new SQL query with one as the projection
     *
     * @return select(1)
     */
    public abstract AbstractSQLQuery<Integer, ?> selectOne();

    /**
     * Create a new SQL query with the given projection and source
     *
     * @param expr query source and projection
     * @param <T>  type of the projection
     * @return select(expr).from(expr)
     */
    public abstract <T> AbstractSQLQuery<T, ?> selectFrom(RelationalPath<T> expr);
}
