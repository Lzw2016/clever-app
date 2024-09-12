package org.clever.data.jdbc.querydsl.sql;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.QueryResults;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLTemplates;
import org.clever.data.jdbc.support.SqlLoggerUtils;

import java.sql.Connection;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基于 {@link com.querydsl.sql.SQLQueryFactory} 的改造
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/09/12 11:04 <br/>
 */
public class SQLQuery<T> extends com.querydsl.sql.SQLQuery<T> {
    public SQLQuery() {
        super();
    }

    public SQLQuery(SQLTemplates templates) {
        super(templates);
    }

    public SQLQuery(Connection conn, SQLTemplates templates) {
        super(conn, templates);
    }

    public SQLQuery(Connection conn, SQLTemplates templates, QueryMetadata metadata) {
        super(conn, templates, metadata);
    }

    public SQLQuery(Configuration configuration) {
        super(configuration);
    }

    public SQLQuery(Connection conn, Configuration configuration) {
        super(conn, configuration);
    }

    public SQLQuery(Connection conn, Configuration configuration, QueryMetadata metadata) {
        super(conn, configuration, metadata);
    }

    public SQLQuery(Supplier<Connection> connProvider, Configuration configuration) {
        super(connProvider, configuration);
    }

    public SQLQuery(Supplier<Connection> connProvider, Configuration configuration, QueryMetadata metadata) {
        super(connProvider, configuration, metadata);
    }

    @Override
    public List<T> fetch() {
        List<T> list = super.fetch();
        SqlLoggerUtils.printfTotal(list.size());
        return list;
    }

    @Override
    public QueryResults<T> fetchResults() {
        QueryResults<T> results = super.fetchResults();
        SqlLoggerUtils.printfTotal(results.getResults().size());
        return results;
    }
}
