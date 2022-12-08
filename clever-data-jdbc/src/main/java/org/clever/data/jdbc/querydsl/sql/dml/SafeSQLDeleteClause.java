package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.SQLDeleteClause;

import java.sql.Connection;
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
}
