package org.clever.data.jdbc.listener;

import org.clever.core.Ordered;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/05 19:01 <br/>
 */
public interface JdbcListener {
    /**
     * 在执行sql之前的操作
     */
    void beforeExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate);

    /**
     * 在执行sql之后的操作
     */
    void afterExec(DbType dbType, NamedParameterJdbcTemplate jdbcTemplate, Exception exception);

    /**
     * 返回监听器执行顺序
     */
    default double getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
