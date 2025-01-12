package org.clever.data.jdbc.support.sqlparser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.thread.ThreadUtils;
import org.clever.data.jdbc.support.sqlparser.cache.JSqlParseCache;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 19:02 <br/>
 */
public class GlobalSqlParser {
    /**
     * 默认线程数大小
     */
    private static final int DEFAULT_THREAD_SIZE = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
    /**
     * sql解析处理线程池
     */
    public static volatile ThreadPoolExecutor EXECUTOR_SERVICE = ThreadUtils.createThreadPool(
        DEFAULT_THREAD_SIZE,
        DEFAULT_THREAD_SIZE,
        new ArrayBlockingQueue<>(64),
        "jsqlparser-%d"
    );
    /**
     * sql解析结果缓存
     */
    public static volatile JSqlParseCache SQL_PARSE_CACHE;
    /**
     * 解析Statement实现
     */
    public static volatile JSqlParserFunction<String, Statement> PARSE_STATEMENT = sql -> CCJSqlParserUtil.parse(sql, EXECUTOR_SERVICE, null);
    /**
     * 解析Statements实现
     */
    public static volatile JSqlParserFunction<String, Statements> PARSE_STATEMENTS = sql -> CCJSqlParserUtil.parseStatements(sql, EXECUTOR_SERVICE, null);

    static {
        AppShutdownHook.addShutdownHook(EXECUTOR_SERVICE::shutdownNow, OrderIncrement.NORMAL - 100, "停止SQL解析");
    }

    /**
     * 解析sql得到Statement
     */
    public static Statement parse(String sql) throws JSQLParserException {
        if (SQL_PARSE_CACHE == null) {
            return PARSE_STATEMENT.parse(sql);
        }
        Statement statement = SQL_PARSE_CACHE.getStatement(sql);
        if (statement == null) {
            statement = PARSE_STATEMENT.parse(sql);
            SQL_PARSE_CACHE.putStatement(sql, statement);
        }
        return statement;
    }

    /**
     * 解析sql得到Statements
     */
    public static Statements parses(String sql) throws JSQLParserException {
        if (SQL_PARSE_CACHE == null) {
            return PARSE_STATEMENTS.parse(sql);
        }
        Statements statements = SQL_PARSE_CACHE.getStatements(sql);
        if (statements == null) {
            statements = PARSE_STATEMENTS.parse(sql);
            SQL_PARSE_CACHE.putStatements(sql, statements);
        }
        return statements;
    }
}
