package org.clever.data.jdbc.support.sqlparser.cache;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;

/**
 * jsqlparser 缓存
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 18:28 <br/>
 */
public interface JSqlParseCache {
    /**
     * 缓存 Statement 对象
     */
    void putStatement(String sql, Statement value);

    /**
     * 缓存 Statements 对象
     */
    void putStatements(String sql, Statements value);

    /**
     * 读取缓存 Statement 对象
     */
    Statement getStatement(String sql);

    /**
     * 读取缓存 Statements 对象
     */
    Statements getStatements(String sql);
}
