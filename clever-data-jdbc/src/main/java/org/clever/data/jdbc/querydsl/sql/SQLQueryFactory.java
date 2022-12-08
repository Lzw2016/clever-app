package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * 基于 {@link com.querydsl.sql.SQLQueryFactory} 的改造，
 * 使用 {@link AbstractSQLQueryFactory} 替换默认的实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/28 22:08 <br/>
 */
public class SQLQueryFactory extends AbstractSQLQueryFactory<SQLQuery<?>> {
    public SQLQueryFactory(Configuration configuration, Supplier<Connection> connProvider) {
        super(configuration, connProvider);
    }

    @Override
    public SQLQuery<?> query() {
        return new SQLQuery<Void>(connection, configuration);
    }

    @Override
    public <T> SQLQuery<T> select(Expression<T> expr) {
        return query().select(expr);
    }

    @Override
    public SQLQuery<Tuple> select(Expression<?>... exprs) {
        return query().select(exprs);
    }

    @Override
    public <T> SQLQuery<T> selectDistinct(Expression<T> expr) {
        return query().select(expr).distinct();
    }

    @Override
    public SQLQuery<Tuple> selectDistinct(Expression<?>... exprs) {
        return query().select(exprs).distinct();
    }

    @Override
    public SQLQuery<Integer> selectZero() {
        return select(Expressions.ZERO);
    }

    @Override
    public SQLQuery<Integer> selectOne() {
        return select(Expressions.ONE);
    }

    @Override
    public <T> SQLQuery<T> selectFrom(RelationalPath<T> expr) {
        return select(expr).from(expr);
    }
}
