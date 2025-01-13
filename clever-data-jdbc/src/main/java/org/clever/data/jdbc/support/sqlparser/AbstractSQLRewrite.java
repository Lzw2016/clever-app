package org.clever.data.jdbc.support.sqlparser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.tuples.TupleTwo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * sql重写抽象类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/13 11:50 <br/>
 */
public abstract class AbstractSQLRewrite implements SQLRewrite {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public TupleTwo<String, Map<String, Object>> rewrite(String rawSql, Object params) {
        if (StringUtils.contains(rawSql, Keywords.SEMICOLON)) {
            return parserMulti(rawSql, params);
        }
        return parserSingle(rawSql, params);
    }

    protected TupleTwo<String, Map<String, Object>> parserSingle(String rawSql, Object params) {
        if (log.isDebugEnabled()) {
            log.debug("original SQL: {}", rawSql);
        }
        try {
            Statement statement = GlobalSqlParser.parse(rawSql);
            return doRewrite(statement, 0, rawSql, params);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException(String.format("Failed to process, Error SQL: %s", rawSql), e);
        }
    }

    protected TupleTwo<String, Map<String, Object>> parserMulti(String rawSql, Object params) {
        if (log.isDebugEnabled()) {
            log.debug("original SQL: {}", rawSql);
        }
        Map<String, Object> extParams = new HashMap<>();
        try {
            StringBuilder sb = new StringBuilder();
            Statements statements = GlobalSqlParser.parses(rawSql);
            int index = 0;
            for (Statement statement : statements) {
                if (index > 0) {
                    sb.append(Keywords.SEMICOLON).append("\n");
                }
                // 重写当前语句
                TupleTwo<String, Map<String, Object>> res = doRewrite(statement, index, rawSql, params);
                sb.append(res.getValue1());
                if (res.getValue2() != null) {
                    extParams.putAll(res.getValue2());
                }
                index++;
            }
            return TupleTwo.creat(sb.toString(), extParams);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException(String.format("Failed to process, Error SQL: %s", rawSql), e);
        }
    }

    protected TupleTwo<String, Map<String, Object>> doRewrite(Statement statement, int index, String rawSql, Object params) {
        if (log.isDebugEnabled()) {
            log.debug("SQL to parse, SQL: {}", rawSql);
        }
        Map<String, Object> extParams = new HashMap<>();
        if (statement instanceof Insert) {
            extParams = this.rewriteInsert((Insert) statement, index, rawSql, params);
        } else if (statement instanceof Select) {
            extParams = this.rewriteSelect((Select) statement, index, rawSql, params);
        } else if (statement instanceof Update) {
            extParams = this.rewriteUpdate((Update) statement, index, rawSql, params);
        } else if (statement instanceof Delete) {
            extParams = this.rewriteDelete((Delete) statement, index, rawSql, params);
        }
        rawSql = statement.toString();
        if (log.isDebugEnabled()) {
            log.debug("parse the finished SQL: {}", rawSql);
        }
        return TupleTwo.creat(rawSql, extParams);
    }

    /**
     * 重写select语句
     *
     * @param select Select对象，直接修改此对象实现sql重写接
     * @param index  当前Select在rawSql的所在位置(当rawSql中含有多条sql语句时很有意义)
     * @param rawSql 原始sql语句
     * @param params 原始sql参数
     * @return 新增的扩展参数
     */
    protected Map<String, Object> rewriteSelect(Select select, int index, String rawSql, Object params) {
        return new HashMap<>();
    }

    /**
     * 重写Insert语句
     *
     * @param insert Insert对象，直接修改此对象实现sql重写接
     * @param index  当前Select在rawSql的所在位置(当rawSql中含有多条sql语句时很有意义)
     * @param rawSql 原始sql语句
     * @param params 原始sql参数
     * @return 新增的扩展参数
     */
    protected Map<String, Object> rewriteInsert(Insert insert, int index, String rawSql, Object params) {
        return new HashMap<>();
    }

    /**
     * 重写Update语句
     *
     * @param update Update对象，直接修改此对象实现sql重写接
     * @param index  当前Select在rawSql的所在位置(当rawSql中含有多条sql语句时很有意义)
     * @param rawSql 原始sql语句
     * @param params 原始sql参数
     * @return 新增的扩展参数
     */
    protected Map<String, Object> rewriteUpdate(Update update, int index, String rawSql, Object params) {
        return new HashMap<>();
    }

    /**
     * 重写Delete语句
     *
     * @param delete Delete对象，直接修改此对象实现sql重写接
     * @param index  当前Select在rawSql的所在位置(当rawSql中含有多条sql语句时很有意义)
     * @param rawSql 原始sql语句
     * @param params 原始sql参数
     * @return 新增的扩展参数
     */
    protected Map<String, Object> rewriteDelete(Delete delete, int index, String rawSql, Object params) {
        return new HashMap<>();
    }
}
