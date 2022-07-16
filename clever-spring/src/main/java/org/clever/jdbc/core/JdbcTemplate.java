package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.dao.support.DataAccessUtils;
import org.clever.jdbc.InvalidResultSetAccessException;
import org.clever.jdbc.SQLWarningException;
import org.clever.jdbc.UncategorizedSQLException;
import org.clever.jdbc.datasource.ConnectionProxy;
import org.clever.jdbc.datasource.DataSourceUtils;
import org.clever.jdbc.support.JdbcAccessor;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.jdbc.support.KeyHolder;
import org.clever.jdbc.support.rowset.SqlRowSet;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;
import org.clever.util.LinkedCaseInsensitiveMap;
import org.clever.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 这是JDBC核心包中的中心类。它简化了JDBC的使用，并有助于避免常见错误。
 * 它执行核心JDBC工作流，留下应用程序代码来提供SQL和提取结果。
 * 这个类执行SQL查询或更新，在结果集上启动迭代，捕获JDBC异常，
 * 并将它们转换为组织中定义的通用的、信息更丰富的异常层次结构{@code org.clever.dao}的包。
 *
 * <p>使用此类的代码只需要实现回调接口，从而为它们提供一个明确定义的契约。
 * {@link PreparedStatementCreator}回调接口在给定连接的情况下创建一个prepared语句，提供SQL和任何必要的参数。
 * {@link ResultSetExtractor}接口从ResultSet中提取值。
 * 另请参阅{@link PreparedStatementSetter}和{@link RowMapper}，了解两种流行的可选回调接口。
 *
 * <p>可以通过数据源引用直接实例化在服务实现中使用，也可以在应用程序上下文中准备并作为bean引用提供给服务。
 * 注意：数据源应始终在应用程序上下文中配置为bean，在第一种情况下直接提供给服务，在第二种情况下提供给准备好的模板。
 *
 * <p>因为这个类可以通过回调接口和@link org.clever.jdbc.support.SQLExceptionTranslator}接口参数化，所以不需要对它进行子类化。
 *
 * <p>该类执行的所有SQL操作都在调试级别记录，使用“org.clever.jdbc.core.JdbcTemplate”作为日志类别。
 *
 * <p><b>注意：该类的实例在配置后是线程安全的。</b>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:41 <br/>
 *
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see CallableStatementCreator
 * @see PreparedStatementCallback
 * @see CallableStatementCallback
 * @see ResultSetExtractor
 * @see RowCallbackHandler
 * @see RowMapper
 * @see org.clever.jdbc.support.SQLExceptionTranslator
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {
    private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";
    private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";

    /**
     * 如果该变量为false，我们将对SQL警告抛出异常。
     */
    private boolean ignoreWarnings = true;
    /**
     * 如果此变量设置为非负值，则它将用于设置用于查询处理的语句的fetchSize属性。
     */
    private int fetchSize = -1;
    /**
     * 如果此变量设置为非负值，则它将用于设置用于查询处理的语句的maxRows属性。
     */
    private int maxRows = -1;
    /**
     * 如果此变量设置为非负值，则它将用于设置用于查询处理的语句的queryTimeout属性。
     */
    private int queryTimeout = -1;
    /**
     * 如果此变量设置为true，则对于任何可调用语句处理，都将绕过所有结果检查。
     * 这可以用来避免一些旧的Oracle JDBC驱动程序（如10.1.0.2）中的错误。
     */
    private boolean skipResultsProcessing = false;
    /**
     * 如果此变量设置为true，则将忽略没有相应SqlOutParameter声明的存储过程调用的所有结果。
     * 除非变量{@code skipResultsProcessing设置为true，否则将进行所有其他结果处理。
     */
    private boolean skipUndeclaredResults = false;
    /**
     * 如果此变量设置为true，则执行CallableStatement将在映射中返回结果，该映射使用不区分大小写的参数名称。
     */
    private boolean resultsMapCaseInsensitive = false;

    /**
     * 构造一个新的JdbcTemplate。
     * <p>注意：在使用实例之前必须设置数据源。
     *
     * @see #setDataSource
     */
    public JdbcTemplate() {
    }

    /**
     * 构造一个新的JdbcTemplate，给定一个数据源来获取连接。
     * <p>注意：这不会触发异常转换器的初始化。
     *
     * @param dataSource 从中获取连接的JDBC数据源
     */
    public JdbcTemplate(DataSource dataSource) {
        setDataSource(dataSource);
        afterPropertiesSet();
    }

    /**
     * 构造一个新的JdbcTemplate，给定一个数据源来获取连接。
     * <p>注意：根据“lazyInit”标志，将触发异常转换器的初始化。
     *
     * @param dataSource 从中获取连接的JDBC数据源
     * @param lazyInit   是否延迟初始化SQLExceptionTranslator
     */
    public JdbcTemplate(DataSource dataSource, boolean lazyInit) {
        setDataSource(dataSource);
        setLazyInit(lazyInit);
        afterPropertiesSet();
    }

    /**
     * 设置是否要忽略SQLWarnings。
     * <p>默认值为“true”，接受并记录所有警告。
     * 将此标志切换为“false”以使JdbcTemplate引发SQLWarningException。
     *
     * @see java.sql.SQLWarning
     * @see org.clever.jdbc.SQLWarningException
     * @see #handleWarnings
     */
    public void setIgnoreWarnings(boolean ignoreWarnings) {
        this.ignoreWarnings = ignoreWarnings;
    }

    /**
     * 返回是否忽略SQLWarnings。
     */
    public boolean isIgnoreWarnings() {
        return this.ignoreWarnings;
    }

    /**
     * 设置此JdbcTemplate的fetchSize。
     * 这对于处理大型结果集很重要：将其设置为高于默认值将提高处理速度，但会消耗内存；
     * 设置此下限可以避免传输应用程序永远不会读取的行数据。
     * <p>默认值为-1，表示使用JDBC驱动程序的默认配置（即不将特定的提取大小设置传递给驱动程序）。
     * <p>注意：除-1之外的负值将传递给驱动程序，因为MySQL支持{@code Integer.MIN_VALUE}.
     *
     * @see java.sql.Statement#setFetchSize
     */
    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * 返回为此JdbcTemplate指定的fetchSize。
     */
    public int getFetchSize() {
        return this.fetchSize;
    }

    /**
     * 设置此JdbcTemplate的最大行数。
     * 这对于处理大型结果集的子集很重要，如果我们一开始对整个结果不感兴趣（例如，在执行可能返回大量匹配的搜索时），
     * 则避免在数据库或JDBC驱动程序中读取和保留整个结果集。
     * <p>默认值为-1，表示使用JDBC驱动程序的默认配置（即不将特定的最大行数设置传递给驱动程序）。
     * <p>注意：除-1之外的负值将传递给驱动程序，与{@link #setFetchSize}对特殊MySQL值的支持同步。
     *
     * @see java.sql.Statement#setMaxRows
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * 返回为此JdbcTemplate指定的最大行数。
     */
    public int getMaxRows() {
        return this.maxRows;
    }

    /**
     * 设置此JdbcTemplate执行的语句的查询超时。
     * <p>默认值为-1，表示使用JDBC驱动程序的默认值（即不传递驱动程序上的特定查询超时设置）。
     * <p>注意：在事务级别指定了超时的事务内执行时，此处指定的任何超时将被剩余的事务超时覆盖。
     *
     * @see java.sql.Statement#setQueryTimeout
     */
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    /**
     * 返回此JdbcTemplate执行的语句的查询超时。
     */
    public int getQueryTimeout() {
        return this.queryTimeout;
    }

    /**
     * 设置是否应跳过结果处理。
     * 当我们知道没有结果被传回时，可以用来优化可调用语句的处理out参数的处理仍然会发生。
     * 这可以用来避免一些旧的Oracle JDBC驱动程序（如10.1.0.2）中的错误。
     */
    public void setSkipResultsProcessing(boolean skipResultsProcessing) {
        this.skipResultsProcessing = skipResultsProcessing;
    }

    /**
     * 返回是否应跳过结果处理。
     */
    public boolean isSkipResultsProcessing() {
        return this.skipResultsProcessing;
    }

    /**
     * 设置是否应跳过未声明的结果。
     */
    public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
        this.skipUndeclaredResults = skipUndeclaredResults;
    }

    /**
     * 返回是否应跳过未声明的结果。
     */
    public boolean isSkipUndeclaredResults() {
        return this.skipUndeclaredResults;
    }

    /**
     * 设置CallableStatement的执行是否会在使用不区分大小写的参数名称的Map中返回结果。
     */
    public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
        this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
    }

    /**
     * 返回CallableStatement的执行是否会在使用不区分大小写的参数名称的Map中返回结果。
     */
    public boolean isResultsMapCaseInsensitive() {
        return this.resultsMapCaseInsensitive;
    }

    //-------------------------------------------------------------------------
    // Methods dealing with a plain java.sql.Connection
    //-------------------------------------------------------------------------

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");
        Connection con = DataSourceUtils.getConnection(obtainDataSource());
        try {
            // Create close-suppressing Connection proxy, also preparing returned Statements.
            Connection conToUse = createConnectionProxy(con);
            return action.doInConnection(conToUse);
        } catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            String sql = getSql(action);
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw translateException("ConnectionCallback", sql, ex);
        } finally {
            DataSourceUtils.releaseConnection(con, getDataSource());
        }
    }

    /**
     * 为给定的JDBC连接创建封闭抑制代理。由{@code execute}方法调用。
     * <p>代理还准备返回的JDBC语句，应用语句设置，如fetchSize、最大行数和查询超时。
     *
     * @param con 用于创建代理的JDBC连接
     * @return 连接代理
     * @see java.sql.Connection#close()
     * @see #execute(ConnectionCallback)
     * @see #applyStatementSettings
     */
    protected Connection createConnectionProxy(Connection con) {
        return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(), new Class<?>[]{ConnectionProxy.class}, new CloseSuppressingInvocationHandler(con));
    }

    //-------------------------------------------------------------------------
    // Methods dealing with static SQL (java.sql.Statement)
    //-------------------------------------------------------------------------

    private <T> T execute(StatementCallback<T> action, boolean closeResources) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");
        Connection con = DataSourceUtils.getConnection(obtainDataSource());
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            applyStatementSettings(stmt);
            T result = action.doInStatement(stmt);
            handleWarnings(stmt);
            return result;
        } catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            String sql = getSql(action);
            JdbcUtils.closeStatement(stmt);
            stmt = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw translateException("StatementCallback", sql, ex);
        } finally {
            if (closeResources) {
                JdbcUtils.closeStatement(stmt);
                DataSourceUtils.releaseConnection(con, getDataSource());
            }
        }
    }

    @Override
    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        return execute(action, true);
    }

    @Override
    public void execute(final String sql) throws DataAccessException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL statement [" + sql + "]");
        }
        // 回调以执行语句。
        class ExecuteStatementCallback implements StatementCallback<Object>, SqlProvider {
            @Override
            public Object doInStatement(Statement stmt) throws SQLException {
                stmt.execute(sql);
                return null;
            }

            @Override
            public String getSql() {
                return sql;
            }
        }
        execute(new ExecuteStatementCallback(), true);
    }

    @Override
    public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
        Assert.notNull(sql, "SQL must not be null");
        Assert.notNull(rse, "ResultSetExtractor must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL query [" + sql + "]");
        }
        // 回调以执行查询。
        class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
            @Override
            public T doInStatement(Statement stmt) throws SQLException {
                ResultSet rs = null;
                try {
                    rs = stmt.executeQuery(sql);
                    return rse.extractData(rs);
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }

            @Override
            public String getSql() {
                return sql;
            }
        }
        return execute(new QueryStatementCallback(), true);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        query(sql, new RowCallbackHandlerResultSetExtractor(rch));
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return result(query(sql, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        class StreamStatementCallback implements StatementCallback<Stream<T>>, SqlProvider {
            @Override
            public Stream<T> doInStatement(Statement stmt) throws SQLException {
                ResultSet rs = stmt.executeQuery(sql);
                Connection con = stmt.getConnection();
                return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
                    JdbcUtils.closeResultSet(rs);
                    JdbcUtils.closeStatement(stmt);
                    DataSourceUtils.releaseConnection(con, getDataSource());
                });
            }

            @Override
            public String getSql() {
                return sql;
            }
        }
        return result(execute(new StreamStatementCallback(), false));
    }

    @Override
    public Map<String, Object> queryForMap(String sql) throws DataAccessException {
        return result(queryForObject(sql, getColumnMapRowMapper()));
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        List<T> results = query(sql, rowMapper);
        return DataAccessUtils.nullableSingleResult(results);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
        return queryForObject(sql, getSingleColumnRowMapper(requiredType));
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
        return query(sql, getSingleColumnRowMapper(elementType));
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
        return query(sql, getColumnMapRowMapper());
    }

    @Override
    public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
        return result(query(sql, new SqlRowSetResultSetExtractor()));
    }

    @Override
    public int update(final String sql) throws DataAccessException {
        Assert.notNull(sql, "SQL must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL update [" + sql + "]");
        }
        // 回调以执行update语句。
        class UpdateStatementCallback implements StatementCallback<Integer>, SqlProvider {
            @Override
            public Integer doInStatement(Statement stmt) throws SQLException {
                int rows = stmt.executeUpdate(sql);
                if (logger.isTraceEnabled()) {
                    logger.trace("SQL update affected " + rows + " rows");
                }
                return rows;
            }

            @Override
            public String getSql() {
                return sql;
            }
        }
        return updateCount(execute(new UpdateStatementCallback(), true));
    }

    @Override
    public int[] batchUpdate(final String... sql) throws DataAccessException {
        Assert.notEmpty(sql, "SQL array must not be empty");
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL batch update of " + sql.length + " statements");
        }
        // 回调以执行批更新。
        class BatchUpdateStatementCallback implements StatementCallback<int[]>, SqlProvider {
            private String currSql;

            @Override
            public int[] doInStatement(Statement stmt) throws SQLException, DataAccessException {
                int[] rowsAffected = new int[sql.length];
                if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
                    for (String sqlStmt : sql) {
                        this.currSql = appendSql(this.currSql, sqlStmt);
                        stmt.addBatch(sqlStmt);
                    }
                    try {
                        rowsAffected = stmt.executeBatch();
                    } catch (BatchUpdateException ex) {
                        String batchExceptionSql = null;
                        for (int i = 0; i < ex.getUpdateCounts().length; i++) {
                            if (ex.getUpdateCounts()[i] == Statement.EXECUTE_FAILED) {
                                batchExceptionSql = appendSql(batchExceptionSql, sql[i]);
                            }
                        }
                        if (StringUtils.hasLength(batchExceptionSql)) {
                            this.currSql = batchExceptionSql;
                        }
                        throw ex;
                    }
                } else {
                    for (int i = 0; i < sql.length; i++) {
                        this.currSql = sql[i];
                        if (!stmt.execute(sql[i])) {
                            rowsAffected[i] = stmt.getUpdateCount();
                        } else {
                            throw new InvalidDataAccessApiUsageException("Invalid batch SQL statement: " + sql[i]);
                        }
                    }
                }
                return rowsAffected;
            }

            private String appendSql(String sql, String statement) {
                return (StringUtils.hasLength(sql) ? sql + "; " + statement : statement);
            }

            @Override
            public String getSql() {
                return this.currSql;
            }
        }
        int[] result = execute(new BatchUpdateStatementCallback(), true);
        Assert.state(result != null, "No update counts");
        return result;
    }

    //-------------------------------------------------------------------------
    // Methods dealing with prepared statements
    //-------------------------------------------------------------------------

    @SuppressWarnings("DuplicatedCode")
    private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources) throws DataAccessException {
        Assert.notNull(psc, "PreparedStatementCreator must not be null");
        Assert.notNull(action, "Callback object must not be null");
        if (logger.isDebugEnabled()) {
            String sql = getSql(psc);
            logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
        }
        Connection con = DataSourceUtils.getConnection(obtainDataSource());
        PreparedStatement ps = null;
        try {
            ps = psc.createPreparedStatement(con);
            applyStatementSettings(ps);
            T result = action.doInPreparedStatement(ps);
            handleWarnings(ps);
            return result;
        } catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            if (psc instanceof ParameterDisposer) {
                ((ParameterDisposer) psc).cleanupParameters();
            }
            String sql = getSql(psc);
            psc = null;
            JdbcUtils.closeStatement(ps);
            ps = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw translateException("PreparedStatementCallback", sql, ex);
        } finally {
            if (closeResources) {
                if (psc instanceof ParameterDisposer) {
                    ((ParameterDisposer) psc).cleanupParameters();
                }
                JdbcUtils.closeStatement(ps);
                DataSourceUtils.releaseConnection(con, getDataSource());
            }
        }
    }

    @Override
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException {
        return execute(psc, action, true);
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        return execute(new SimplePreparedStatementCreator(sql), action, true);
    }

    /**
     * 使用prepared语句进行查询，允许PreparedStatementCreator和PreparedStatementSetter。
     * 大多数其他查询方法都使用此方法，但应用程序代码将始终与创建者或setter一起工作。
     *
     * @param psc 在给定连接的情况下创建PreparedStatement的回调
     * @param pss 知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     * @param rse 将提取结果的回调
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果有任何问题
     */
    public <T> T query(PreparedStatementCreator psc, final PreparedStatementSetter pss, final ResultSetExtractor<T> rse) throws DataAccessException {
        Assert.notNull(rse, "ResultSetExtractor must not be null");
        logger.debug("Executing prepared SQL query");
        return execute(psc, ps -> {
            ResultSet rs = null;
            try {
                if (pss != null) {
                    pss.setValues(ps);
                }
                rs = ps.executeQuery();
                return rse.extractData(rs);
            } finally {
                JdbcUtils.closeResultSet(rs);
                if (pss instanceof ParameterDisposer) {
                    ((ParameterDisposer) pss).cleanupParameters();
                }
            }
        }, true);
    }

    @Override
    public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(psc, null, rse);
    }

    @Override
    public <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(new SimplePreparedStatementCreator(sql), pss, rse);
    }

    @Override
    public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
    }

    protected <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, newArgPreparedStatementSetter(args), rse);
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException {
        return query(sql, newArgPreparedStatementSetter(args), rse);
    }

    @Override
    public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
        query(psc, new RowCallbackHandlerResultSetExtractor(rch));
    }

    @Override
    public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
        query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch));
    }

    @Override
    public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
        query(sql, newArgTypePreparedStatementSetter(args, argTypes), rch);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException {
        query(sql, newArgPreparedStatementSetter(args), rch);
    }

    @Override
    public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
        return result(query(psc, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    @Override
    public <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
        return result(query(sql, pss, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    @Override
    public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
        return result(query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    protected <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return result(query(sql, args, new RowMapperResultSetExtractor<>(rowMapper)));
    }

    /**
     * 使用prepared语句进行查询，允许PreparedStatementCreator和PreparedStatementSetter。
     * 大多数其他查询方法都使用此方法，但应用程序代码将始终与创建者或setter一起工作。
     *
     * @param psc       在给定连接的情况下创建PreparedStatement的回调
     * @param pss       知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果查询失败
     */
    public <T> Stream<T> queryForStream(PreparedStatementCreator psc, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
        return result(execute(psc, ps -> {
            if (pss != null) {
                pss.setValues(ps);
            }
            ResultSet rs = ps.executeQuery();
            Connection con = ps.getConnection();
            return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
                JdbcUtils.closeResultSet(rs);
                if (pss instanceof ParameterDisposer) {
                    ((ParameterDisposer) pss).cleanupParameters();
                }
                JdbcUtils.closeStatement(ps);
                DataSourceUtils.releaseConnection(con, getDataSource());
            });
        }, false));
    }

    @Override
    public <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
        return queryForStream(psc, null, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
        return queryForStream(new SimplePreparedStatementCreator(sql), pss, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return queryForStream(new SimplePreparedStatementCreator(sql), newArgPreparedStatementSetter(args), rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
        List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(results);
    }

    protected <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(results);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        List<T> results = query(sql, args, new RowMapperResultSetExtractor<>(rowMapper, 1));
        return DataAccessUtils.nullableSingleResult(results);
    }

    @Override
    public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType) throws DataAccessException {
        return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType));
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
        return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return result(queryForObject(sql, args, argTypes, getColumnMapRowMapper()));
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
        return result(queryForObject(sql, args, getColumnMapRowMapper()));
    }

    @Override
    public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
        return query(sql, args, argTypes, getSingleColumnRowMapper(elementType));
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
        return query(sql, args, getSingleColumnRowMapper(elementType));
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return query(sql, args, argTypes, getColumnMapRowMapper());
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
        return query(sql, args, getColumnMapRowMapper());
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return result(query(sql, args, argTypes, new SqlRowSetResultSetExtractor()));
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException {
        return result(query(sql, args, new SqlRowSetResultSetExtractor()));
    }

    protected int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss) throws DataAccessException {
        logger.debug("Executing prepared SQL update");
        return updateCount(execute(psc, ps -> {
            try {
                if (pss != null) {
                    pss.setValues(ps);
                }
                int rows = ps.executeUpdate();
                if (logger.isTraceEnabled()) {
                    logger.trace("SQL update affected " + rows + " rows");
                }
                return rows;
            } finally {
                if (pss instanceof ParameterDisposer) {
                    ((ParameterDisposer) pss).cleanupParameters();
                }
            }
        }, true));
    }

    @Override
    public int update(PreparedStatementCreator psc) throws DataAccessException {
        return update(psc, (PreparedStatementSetter) null);
    }

    @Override
    public int update(final PreparedStatementCreator psc, final KeyHolder generatedKeyHolder) throws DataAccessException {
        Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
        logger.debug("Executing SQL update and returning generated keys");
        return updateCount(execute(psc, ps -> {
            int rows = ps.executeUpdate();
            List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
            generatedKeys.clear();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys != null) {
                try {
                    RowMapperResultSetExtractor<Map<String, Object>> rse = new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), 1);
                    generatedKeys.addAll(result(rse.extractData(keys)));
                } finally {
                    JdbcUtils.closeResultSet(keys);
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeys.size() + " keys");
            }
            return rows;
        }, true));
    }

    @Override
    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
        return update(new SimplePreparedStatementCreator(sql), pss);
    }

    @Override
    public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
        return update(sql, newArgTypePreparedStatementSetter(args, argTypes));
    }

    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        return update(sql, newArgPreparedStatementSetter(args));
    }

    @Override
    public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL batch update [" + sql + "]");
        }
        int[] result = execute(sql, (PreparedStatementCallback<int[]>) ps -> {
            try {
                int batchSize = pss.getBatchSize();
                InterruptibleBatchPreparedStatementSetter ipss = (
                        pss instanceof InterruptibleBatchPreparedStatementSetter ? (InterruptibleBatchPreparedStatementSetter) pss : null
                );
                if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
                    for (int i = 0; i < batchSize; i++) {
                        pss.setValues(ps, i);
                        if (ipss != null && ipss.isBatchExhausted(i)) {
                            break;
                        }
                        ps.addBatch();
                    }
                    return ps.executeBatch();
                } else {
                    List<Integer> rowsAffected = new ArrayList<>();
                    for (int i = 0; i < batchSize; i++) {
                        pss.setValues(ps, i);
                        if (ipss != null && ipss.isBatchExhausted(i)) {
                            break;
                        }
                        rowsAffected.add(ps.executeUpdate());
                    }
                    int[] rowsAffectedArray = new int[rowsAffected.size()];
                    for (int i = 0; i < rowsAffectedArray.length; i++) {
                        rowsAffectedArray[i] = rowsAffected.get(i);
                    }
                    return rowsAffectedArray;
                }
            } finally {
                if (pss instanceof ParameterDisposer) {
                    ((ParameterDisposer) pss).cleanupParameters();
                }
            }
        });
        Assert.state(result != null, "No result array");
        return result;
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        return batchUpdate(sql, batchArgs, new int[0]);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs, final int[] argTypes) throws DataAccessException {
        if (batchArgs.isEmpty()) {
            return new int[0];
        }
        return batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] values = batchArgs.get(i);
                int colIndex = 0;
                for (Object value : values) {
                    colIndex++;
                    if (value instanceof SqlParameterValue) {
                        SqlParameterValue paramValue = (SqlParameterValue) value;
                        StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
                    } else {
                        int colType;
                        if (argTypes.length < colIndex) {
                            colType = SqlTypeValue.TYPE_UNKNOWN;
                        } else {
                            colType = argTypes[colIndex - 1];
                        }
                        StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
                    }
                }
            }

            @Override
            public int getBatchSize() {
                return batchArgs.size();
            }
        });
    }

    @Override
    public <T> int[][] batchUpdate(
            String sql,
            final Collection<T> batchArgs,
            final int batchSize,
            final ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
        }
        int[][] result = execute(sql, (PreparedStatementCallback<int[][]>) ps -> {
            List<int[]> rowsAffected = new ArrayList<>();
            try {
                boolean batchSupported = JdbcUtils.supportsBatchUpdates(ps.getConnection());
                int n = 0;
                for (T obj : batchArgs) {
                    pss.setValues(ps, obj);
                    n++;
                    if (batchSupported) {
                        ps.addBatch();
                        if (n % batchSize == 0 || n == batchArgs.size()) {
                            if (logger.isTraceEnabled()) {
                                int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
                                int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
                                logger.trace("Sending SQL batch update #" + batchIdx + " with " + items + " items");
                            }
                            rowsAffected.add(ps.executeBatch());
                        }
                    } else {
                        int i = ps.executeUpdate();
                        rowsAffected.add(new int[]{i});
                    }
                }
                int[][] result1 = new int[rowsAffected.size()][];
                for (int i = 0; i < result1.length; i++) {
                    result1[i] = rowsAffected.get(i);
                }
                return result1;
            } finally {
                if (pss instanceof ParameterDisposer) {
                    ((ParameterDisposer) pss).cleanupParameters();
                }
            }
        });
        Assert.state(result != null, "No result array");
        return result;
    }

    //-------------------------------------------------------------------------
    // Methods dealing with callable statements
    //-------------------------------------------------------------------------

    @SuppressWarnings("DuplicatedCode")
    @Override
    public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException {
        Assert.notNull(csc, "CallableStatementCreator must not be null");
        Assert.notNull(action, "Callback object must not be null");
        if (logger.isDebugEnabled()) {
            String sql = getSql(csc);
            logger.debug("Calling stored procedure" + (sql != null ? " [" + sql + "]" : ""));
        }
        Connection con = DataSourceUtils.getConnection(obtainDataSource());
        CallableStatement cs = null;
        try {
            cs = csc.createCallableStatement(con);
            applyStatementSettings(cs);
            T result = action.doInCallableStatement(cs);
            handleWarnings(cs);
            return result;
        } catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            if (csc instanceof ParameterDisposer) {
                ((ParameterDisposer) csc).cleanupParameters();
            }
            String sql = getSql(csc);
            csc = null;
            JdbcUtils.closeStatement(cs);
            cs = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw translateException("CallableStatementCallback", sql, ex);
        } finally {
            if (csc instanceof ParameterDisposer) {
                ((ParameterDisposer) csc).cleanupParameters();
            }
            JdbcUtils.closeStatement(cs);
            DataSourceUtils.releaseConnection(con, getDataSource());
        }
    }

    @Override
    public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
        return execute(new SimpleCallableStatementCreator(callString), action);
    }

    @Override
    public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters) throws DataAccessException {
        final List<SqlParameter> updateCountParameters = new ArrayList<>();
        final List<SqlParameter> resultSetParameters = new ArrayList<>();
        final List<SqlParameter> callParameters = new ArrayList<>();
        for (SqlParameter parameter : declaredParameters) {
            if (parameter.isResultsParameter()) {
                if (parameter instanceof SqlReturnResultSet) {
                    resultSetParameters.add(parameter);
                } else {
                    updateCountParameters.add(parameter);
                }
            } else {
                callParameters.add(parameter);
            }
        }
        Map<String, Object> result = execute(csc, cs -> {
            boolean retVal = cs.execute();
            int updateCount = cs.getUpdateCount();
            if (logger.isTraceEnabled()) {
                logger.trace("CallableStatement.execute() returned '" + retVal + "'");
                logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
            }
            Map<String, Object> resultsMap = createResultsMap();
            if (retVal || updateCount != -1) {
                resultsMap.putAll(extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount));
            }
            resultsMap.putAll(extractOutputParameters(cs, callParameters));
            return resultsMap;
        });
        Assert.state(result != null, "No result map");
        return result;
    }

    /**
     * 从已完成的存储过程中提取返回的结果集。
     *
     * @param cs                    存储过程的JDBC包装器
     * @param updateCountParameters 存储过程的已声明更新计数参数的参数列表
     * @param resultSetParameters   存储过程的已声明结果集参数的参数列表
     * @return 包含返回结果的Map
     */
    protected Map<String, Object> extractReturnedResults(CallableStatement cs,
                                                         List<SqlParameter> updateCountParameters,
                                                         List<SqlParameter> resultSetParameters,
                                                         int updateCount) throws SQLException {
        Map<String, Object> results = new LinkedHashMap<>(4);
        int rsIndex = 0;
        int updateIndex = 0;
        boolean moreResults;
        if (!this.skipResultsProcessing) {
            do {
                if (updateCount == -1) {
                    if (resultSetParameters != null && resultSetParameters.size() > rsIndex) {
                        SqlReturnResultSet declaredRsParam = (SqlReturnResultSet) resultSetParameters.get(rsIndex);
                        results.putAll(processResultSet(cs.getResultSet(), declaredRsParam));
                        rsIndex++;
                    } else {
                        if (!this.skipUndeclaredResults) {
                            String rsName = RETURN_RESULT_SET_PREFIX + (rsIndex + 1);
                            SqlReturnResultSet undeclaredRsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
                            if (logger.isTraceEnabled()) {
                                logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
                            }
                            results.putAll(processResultSet(cs.getResultSet(), undeclaredRsParam));
                            rsIndex++;
                        }
                    }
                } else {
                    if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
                        SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
                        String declaredUcName = ucParam.getName();
                        results.put(declaredUcName, updateCount);
                        updateIndex++;
                    } else {
                        if (!this.skipUndeclaredResults) {
                            String undeclaredName = RETURN_UPDATE_COUNT_PREFIX + (updateIndex + 1);
                            if (logger.isTraceEnabled()) {
                                logger.trace("Added default SqlReturnUpdateCount parameter named '" + undeclaredName + "'");
                            }
                            results.put(undeclaredName, updateCount);
                            updateIndex++;
                        }
                    }
                }
                moreResults = cs.getMoreResults();
                updateCount = cs.getUpdateCount();
                if (logger.isTraceEnabled()) {
                    logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
                }
            } while (moreResults || updateCount != -1);
        }
        return results;
    }

    /**
     * 从已完成的存储过程中提取输出参数。
     *
     * @param cs         存储过程的JDBC包装器
     * @param parameters 存储过程的参数列表
     * @return 包含返回结果的Map
     */
    protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters) throws SQLException {
        Map<String, Object> results = CollectionUtils.newLinkedHashMap(parameters.size());
        int sqlColIndex = 1;
        for (SqlParameter param : parameters) {
            if (param instanceof SqlOutParameter) {
                SqlOutParameter outParam = (SqlOutParameter) param;
                Assert.state(outParam.getName() != null, "Anonymous parameters not allowed");
                SqlReturnType returnType = outParam.getSqlReturnType();
                if (returnType != null) {
                    Object out = returnType.getTypeValue(cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
                    results.put(outParam.getName(), out);
                } else {
                    Object out = cs.getObject(sqlColIndex);
                    if (out instanceof ResultSet) {
                        if (outParam.isResultSetSupported()) {
                            results.putAll(processResultSet((ResultSet) out, outParam));
                        } else {
                            String rsName = outParam.getName();
                            SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
                            results.putAll(processResultSet((ResultSet) out, rsParam));
                            if (logger.isTraceEnabled()) {
                                logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
                            }
                        }
                    } else {
                        results.put(outParam.getName(), out);
                    }
                }
            }
            if (!(param.isResultsParameter())) {
                sqlColIndex++;
            }
        }
        return results;
    }

    /**
     * 处理存储过程中的给定结果集。
     *
     * @param rs    选择要处理的结果集
     * @param param 对应的存储过程参数
     * @return 包含返回结果的Map
     */
    protected Map<String, Object> processResultSet(ResultSet rs, ResultSetSupportingSqlParameter param) throws SQLException {
        if (rs != null) {
            try {
                if (param.getRowMapper() != null) {
                    RowMapper<?> rowMapper = param.getRowMapper();
                    Object data = (new RowMapperResultSetExtractor<>(rowMapper)).extractData(rs);
                    return Collections.singletonMap(param.getName(), data);
                } else if (param.getRowCallbackHandler() != null) {
                    RowCallbackHandler rch = param.getRowCallbackHandler();
                    (new RowCallbackHandlerResultSetExtractor(rch)).extractData(rs);
                    return Collections.singletonMap(param.getName(), "ResultSet returned from stored procedure was processed");
                } else if (param.getResultSetExtractor() != null) {
                    Object data = param.getResultSetExtractor().extractData(rs);
                    return Collections.singletonMap(param.getName(), data);
                }
            } finally {
                JdbcUtils.closeResultSet(rs);
            }
        }
        return Collections.emptyMap();
    }

    //-------------------------------------------------------------------------
    // Implementation hooks and helper methods
    //-------------------------------------------------------------------------

    /**
     * 创建一个新的行映射器，用于将列作为键值对进行读取。
     *
     * @return 要使用的行映射器
     * @see ColumnMapRowMapper
     */
    protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
        return new ColumnMapRowMapper();
    }

    /**
     * 创建一个新的行映射器，用于从单个列中读取结果对象。
     *
     * @param requiredType 每个结果对象预期匹配的类型
     * @return 要使用的行映射器
     * @see SingleColumnRowMapper
     */
    protected <T> RowMapper<T> getSingleColumnRowMapper(Class<T> requiredType) {
        return new SingleColumnRowMapper<>(requiredType);
    }

    /**
     * 创建要用作结果映射的映射实例。
     * <p>如果{@link #resultsMapCaseInsensitive}已设置为true，则将创建{@link LinkedCaseInsensitiveMap}；
     * 否则，将创建{@link LinkedHashMap}。
     *
     * @return 结果映射实例
     * @see #setResultsMapCaseInsensitive
     * @see #isResultsMapCaseInsensitive
     */
    protected Map<String, Object> createResultsMap() {
        if (isResultsMapCaseInsensitive()) {
            return new LinkedCaseInsensitiveMap<>();
        } else {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 准备给定的JDBC语句（或PreparedStatement或CallableStatement），应用语句设置，例如fetchSize、最大行数和查询超时。
     *
     * @param stmt 准备JDBC语句
     * @throws SQLException 如果由JDBC API引发
     * @see #setFetchSize
     * @see #setMaxRows
     * @see #setQueryTimeout
     * @see org.clever.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
     */
    protected void applyStatementSettings(Statement stmt) throws SQLException {
        int fetchSize = getFetchSize();
        if (fetchSize != -1) {
            stmt.setFetchSize(fetchSize);
        }
        int maxRows = getMaxRows();
        if (maxRows != -1) {
            stmt.setMaxRows(maxRows);
        }
        DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
    }

    /**
     * 使用传入的args创建一个新的基于arg的PreparedStatementSetter。
     * <p>默认情况下，我们将创建{@link ArgumentPreparedStatementSetter}。
     * 该方法允许创建被子类覆盖。
     *
     * @param args 带参数的对象数组
     * @return 要使用的新PreparedStatementSetter
     */
    protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
        return new ArgumentPreparedStatementSetter(args);
    }

    /**
     * 使用传入的参数和类型创建一个新的基于参数类型的PreparedStatementSetter。
     * <p>默认情况下，我们将创建{@link ArgumentTypePreparedStatementSetter}。
     * 该方法允许创建被子类覆盖。
     *
     * @param args     带参数的对象数组
     * @param argTypes 关联参数的SQLTypes的int数组
     * @return 要使用的新PreparedStatementSetter
     */
    protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        return new ArgumentTypePreparedStatementSetter(args, argTypes);
    }

    /**
     * 如果我们没有忽略警告，则抛出SQLWarningException，否则在调试级别记录警告。
     *
     * @param stmt 当前JDBC语句
     * @throws SQLWarningException 如果不忽略警告
     * @see org.clever.jdbc.SQLWarningException
     */
    protected void handleWarnings(Statement stmt) throws SQLException {
        if (isIgnoreWarnings()) {
            if (logger.isDebugEnabled()) {
                SQLWarning warningToLog = stmt.getWarnings();
                while (warningToLog != null) {
                    logger.debug("SQLWarning ignored: SQL state '" +
                            warningToLog.getSQLState() + "', error code '" +
                            warningToLog.getErrorCode() + "', message [" +
                            warningToLog.getMessage() + "]"
                    );
                    warningToLog = warningToLog.getNextWarning();
                }
            }
        } else {
            handleWarnings(stmt.getWarnings());
        }
    }

    /**
     * 如果遇到实际警告，则抛出SQLWarningException。
     *
     * @param warning 当前语句中的warnings对象。可以为null，在这种情况下，此方法不执行任何操作。
     * @throws SQLWarningException 如果要发出实际警告
     */
    protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
        if (warning != null) {
            throw new SQLWarningException("Warning not ignored", warning);
        }
    }

    /**
     * 将给定的{@link SQLException}转换为通用{@link DataAccessException}。
     *
     * @param task 描述正在尝试的任务的可读文本
     * @param sql  导致问题的SQL查询或更新(可能是 {@code null})
     * @param ex   {@code SQLException}
     * @return DataAccessException包装SQLException（从不为空）
     * @see #getExceptionTranslator()
     */
    protected DataAccessException translateException(String task, String sql, SQLException ex) {
        DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
        return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
    }

    /**
     * 从潜在的提供程序对象确定SQL。
     *
     * @param sqlProvider 对象，该对象可能是SqlProvider
     * @return SQL字符串，如果未知，则为null
     * @see SqlProvider
     */
    private static String getSql(Object sqlProvider) {
        if (sqlProvider instanceof SqlProvider) {
            return ((SqlProvider) sqlProvider).getSql();
        } else {
            return null;
        }
    }

    private static <T> T result(T result) {
        Assert.state(result != null, "No result");
        return result;
    }

    private static int updateCount(Integer result) {
        Assert.state(result != null, "No update count");
        return result;
    }

    /**
     * 抑制JDBC连接上的关闭调用的调用处理程序。还准备返回的语句（Prepared/CallbackStatement）对象。
     *
     * @see java.sql.Connection#close()
     */
    private class CloseSuppressingInvocationHandler implements InvocationHandler {
        private final Connection target;

        public CloseSuppressingInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Invocation on ConnectionProxy interface coming in...
            switch (method.getName()) {
                case "equals":
                    // Only consider equal when proxies are identical.
                    return (proxy == args[0]);
                case "hashCode":
                    // Use hashCode of PersistenceManager proxy.
                    return System.identityHashCode(proxy);
                case "close":
                    // Handle close method: suppress, not valid.
                    return null;
                case "isClosed":
                    return false;
                case "getTargetConnection":
                    // Handle getTargetConnection method: return underlying Connection.
                    return this.target;
                case "unwrap":
                    return (((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
                case "isWrapperFor":
                    return (((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
            }
            // Invoke method on target Connection.
            try {
                Object retVal = method.invoke(this.target, args);
                // If return value is a JDBC Statement, apply statement settings
                // (fetch size, max rows, transaction timeout).
                if (retVal instanceof Statement) {
                    applyStatementSettings(((Statement) retVal));
                }
                return retVal;
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    /**
     * PreparedStatementCreator的简单适配器，允许使用普通SQL语句。
     */
    private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
        private final String sql;

        public SimplePreparedStatementCreator(String sql) {
            Assert.notNull(sql, "SQL must not be null");
            this.sql = sql;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(this.sql);
        }

        @Override
        public String getSql() {
            return this.sql;
        }
    }

    /**
     * CallableStatementCreator的简单适配器，允许使用普通SQL语句。
     */
    private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {
        private final String callString;

        public SimpleCallableStatementCreator(String callString) {
            Assert.notNull(callString, "Call string must not be null");
            this.callString = callString;
        }

        @Override
        public CallableStatement createCallableStatement(Connection con) throws SQLException {
            return con.prepareCall(this.callString);
        }

        @Override
        public String getSql() {
            return this.callString;
        }
    }

    /**
     * 用于在ResultSetExtractor内启用RowCallbackHandler的适配器。
     * <p>使用常规结果集，因此在使用它时必须小心：我们不使用它进行导航，因为这可能会导致不可预测的后果。
     */
    private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<Object> {
        private final RowCallbackHandler rch;

        public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
            this.rch = rch;
        }

        @Override
        public Object extractData(ResultSet rs) throws SQLException {
            while (rs.next()) {
                this.rch.processRow(rs);
            }
            return null;
        }
    }

    /**
     * 用于查询结果集到流的流适配的拆分器。
     */
    private static class ResultSetSpliterator<T> implements Spliterator<T> {
        private final ResultSet rs;
        private final RowMapper<T> rowMapper;
        private int rowNum = 0;

        public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper) {
            this.rs = rs;
            this.rowMapper = rowMapper;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                if (this.rs.next()) {
                    action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
                    return true;
                }
                return false;
            } catch (SQLException ex) {
                throw new InvalidResultSetAccessException(ex);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED;
        }

        public Stream<T> stream() {
            return StreamSupport.stream(this, false);
        }
    }
}
