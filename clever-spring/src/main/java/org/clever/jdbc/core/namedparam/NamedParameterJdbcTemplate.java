package org.clever.jdbc.core.namedparam;

import org.clever.dao.DataAccessException;
import org.clever.dao.support.DataAccessUtils;
import org.clever.jdbc.core.*;
import org.clever.jdbc.support.KeyHolder;
import org.clever.jdbc.support.rowset.SqlRowSet;
import org.clever.util.Assert;
import org.clever.util.ConcurrentLruCache;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 具有一组基本JDBC操作的模板类，允许使用命名参数而不是传统的“？”占位符。
 *
 * <p>将命名参数替换为JDBC样式“？”后，此类将委托给包装的{@link #getJdbcOperations() JdbcTemplate}占位符在执行时完成。
 * 它还允许将值{@link java.util.List}扩展到适当数量的占位符。
 *
 * <p>底层{@link org.clever.jdbc.core.JdbcTemplate}是公开的，允许方便地访问传统的{@link org.clever.jdbc.core.JdbcTemplate}方法。
 *
 * <p><b>注意：该类的实例在配置后是线程安全的。</b>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:46 <br/>
 *
 * @see NamedParameterJdbcOperations
 * @see org.clever.jdbc.core.JdbcTemplate
 */
public class NamedParameterJdbcTemplate implements NamedParameterJdbcOperations {
    /**
     * 此模板的SQL缓存的默认最大项数：256。
     */
    public static final int DEFAULT_CACHE_LIMIT = 256;
    /**
     * 我们正在包装的JdbcTemplate。
     */
    private final JdbcOperations classicJdbcTemplate;
    /**
     * 将原始SQL字符串缓存到ParsedSql表示。
     */
    private volatile ConcurrentLruCache<String, ParsedSql> parsedSqlCache = new ConcurrentLruCache<>(
            DEFAULT_CACHE_LIMIT, NamedParameterUtils::parseSqlStatement
    );

    /**
     * 为给定{@link DataSource}创建新的NamedParameterJdbcTemplate。
     * <p>创建一个经典的{@link org.clever.jdbc.core.JdbcTemplate}并包装它。
     *
     * @param dataSource 要访问的JDBC数据源
     */
    public NamedParameterJdbcTemplate(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource must not be null");
        this.classicJdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 为给定的经典{@link org.clever.jdbc.core.JdbcTemplate}创建一个新的NamedParameterJdbcTemplate。
     *
     * @param classicJdbcTemplate 要包装的经典JdbcTemplate
     */
    public NamedParameterJdbcTemplate(JdbcOperations classicJdbcTemplate) {
        Assert.notNull(classicJdbcTemplate, "JdbcTemplate must not be null");
        this.classicJdbcTemplate = classicJdbcTemplate;
    }

    /**
     * 公开经典的JdbcTemplate操作，以允许调用不太常用的方法。
     */
    @Override
    public JdbcOperations getJdbcOperations() {
        return this.classicJdbcTemplate;
    }

    /**
     * 公开经典的{@link JdbcTemplate}本身（如果可用），特别是将其传递给其他{@code JdbcTemplate}使用者。
     * <p>如果足以满足当前的目的，建议使用{@link #getJdbcOperations()}。
     */
    public JdbcTemplate getJdbcTemplate() {
        Assert.state(this.classicJdbcTemplate instanceof JdbcTemplate, "No JdbcTemplate available");
        return (JdbcTemplate) this.classicJdbcTemplate;
    }

    /**
     * 指定此模板的SQL缓存的最大项数。
     * 默认值为256。0表示没有缓存，始终解析每个语句。
     */
    public void setCacheLimit(int cacheLimit) {
        this.parsedSqlCache = new ConcurrentLruCache<>(cacheLimit, NamedParameterUtils::parseSqlStatement);
    }

    /**
     * 返回此模板的SQL缓存的最大项数。
     */
    public int getCacheLimit() {
        return this.parsedSqlCache.sizeLimit();
    }

    @Override
    public <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action) throws DataAccessException {
        return getJdbcOperations().execute(getPreparedStatementCreator(sql, paramSource), action);
    }

    @Override
    public <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action) throws DataAccessException {
        return execute(sql, new MapSqlParameterSource(paramMap), action);
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        return execute(sql, EmptySqlParameterSource.INSTANCE, action);
    }

    @Override
    public <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse) throws DataAccessException {
        return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rse);
    }

    @Override
    public <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, new MapSqlParameterSource(paramMap), rse);
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, EmptySqlParameterSource.INSTANCE, rse);
    }

    @Override
    public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch) throws DataAccessException {
        getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rch);
    }

    @Override
    public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException {
        query(sql, new MapSqlParameterSource(paramMap), rch);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
        query(sql, EmptySqlParameterSource.INSTANCE, rch);
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException {
        return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, new MapSqlParameterSource(paramMap), rowMapper);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, EmptySqlParameterSource.INSTANCE, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException {
        return getJdbcOperations().queryForStream(getPreparedStatementCreator(sql, paramSource), rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return queryForStream(sql, new MapSqlParameterSource(paramMap), rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException {
        List<T> results = getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
        return DataAccessUtils.nullableSingleResult(results);
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException {
        return queryForObject(sql, new MapSqlParameterSource(paramMap), rowMapper);
    }

    @Override
    public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType) throws DataAccessException {
        return queryForObject(sql, paramSource, new SingleColumnRowMapper<>(requiredType));
    }

    @Override
    public <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType) throws DataAccessException {
        return queryForObject(sql, paramMap, new SingleColumnRowMapper<>(requiredType));
    }

    @Override
    public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
        Map<String, Object> result = queryForObject(sql, paramSource, new ColumnMapRowMapper());
        Assert.state(result != null, "No result map");
        return result;
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
        Map<String, Object> result = queryForObject(sql, paramMap, new ColumnMapRowMapper());
        Assert.state(result != null, "No result map");
        return result;
    }

    @Override
    public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType) throws DataAccessException {
        return query(sql, paramSource, new SingleColumnRowMapper<>(elementType));
    }

    @Override
    public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType) throws DataAccessException {
        return queryForList(sql, new MapSqlParameterSource(paramMap), elementType);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return query(sql, paramSource, new ColumnMapRowMapper());
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return queryForList(sql, new MapSqlParameterSource(paramMap));
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
        SqlRowSet result = getJdbcOperations().query(
                getPreparedStatementCreator(sql, paramSource), new SqlRowSetResultSetExtractor()
        );
        Assert.state(result != null, "No result");
        return result;
    }

    @Override
    public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return queryForRowSet(sql, new MapSqlParameterSource(paramMap));
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
        return getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource));
    }

    @Override
    public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
        return update(sql, new MapSqlParameterSource(paramMap));
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder) throws DataAccessException {
        return update(sql, paramSource, generatedKeyHolder, null);
    }

    @Override
    public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames) throws DataAccessException {
        PreparedStatementCreator psc = getPreparedStatementCreator(sql, paramSource, pscf -> {
            if (keyColumnNames != null) {
                pscf.setGeneratedKeysColumnNames(keyColumnNames);
            } else {
                pscf.setReturnGeneratedKeys(true);
            }
        });
        return getJdbcOperations().update(psc, generatedKeyHolder);
    }

    @Override
    public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
        return batchUpdate(sql, SqlParameterSourceUtils.createBatch(batchValues));
    }

    @Override
    public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
        if (batchArgs.length == 0) {
            return new int[0];
        }
        ParsedSql parsedSql = getParsedSql(sql);
        PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, batchArgs[0]);
        return getJdbcOperations().batchUpdate(
                pscf.getSql(),
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
                        pscf.newPreparedStatementSetter(values).setValues(ps);
                    }

                    @Override
                    public int getBatchSize() {
                        return batchArgs.length;
                    }
                }
        );
    }

    /**
     * 基于给定的SQL和命名参数构建{@link PreparedStatementCreator}。
     * <p>注意：直接从所有查询变量调用。委托给常见的{@link #getPreparedStatementCreator(String, SqlParameterSource, Consumer)}方法
     *
     * @param sql         要执行的SQL语句
     * @param paramSource 要绑定的参数容器
     * @return 相应的 {@link PreparedStatementCreator}
     * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
     */
    protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
        return getPreparedStatementCreator(sql, paramSource, null);
    }

    /**
     * 基于给定的SQL和命名参数构建{@link PreparedStatementCreator}。
     * <p>注意：用于具有生成的密钥处理的更新变量，也可以从{@link #getPreparedStatementCreator(String, SqlParameterSource)}委派。
     *
     * @param sql         要执行的SQL语句
     * @param paramSource 要绑定的参数容器
     * @param customizer  用于在使用中的{@link PreparedStatementCreatorFactory}上设置更多属性的回调，在实际的{@code newPreparedStatementCreator}调用之前应用
     * @return 相应的 {@link PreparedStatementCreator}
     * @see #getParsedSql(String)
     * @see PreparedStatementCreatorFactory#PreparedStatementCreatorFactory(String, List)
     * @see PreparedStatementCreatorFactory#newPreparedStatementCreator(Object[])
     */
    protected PreparedStatementCreator getPreparedStatementCreator(String sql,
                                                                   SqlParameterSource paramSource,
                                                                   Consumer<PreparedStatementCreatorFactory> customizer) {
        ParsedSql parsedSql = getParsedSql(sql);
        PreparedStatementCreatorFactory pscf = getPreparedStatementCreatorFactory(parsedSql, paramSource);
        if (customizer != null) {
            customizer.accept(pscf);
        }
        Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
        return pscf.newPreparedStatementCreator(params);
    }

    /**
     * 获取给定SQL语句的解析表示。
     * <p>默认实现使用上限为256个条目的LRU缓存。
     *
     * @param sql 原始SQL语句
     * @return 已解析SQL语句的表示形式
     */
    protected ParsedSql getParsedSql(String sql) {
        return this.parsedSqlCache.get(sql);
    }

    /**
     * 基于给定的SQL和命名参数构建{@link PreparedStatementCreatorFactory}。
     *
     * @param parsedSql   给定SQL语句的解析表示形式
     * @param paramSource 要绑定的参数容器
     * @return 相应的 {@link PreparedStatementCreatorFactory}
     * @see #getPreparedStatementCreator(String, SqlParameterSource, Consumer)
     * @see #getParsedSql(String)
     */
    protected PreparedStatementCreatorFactory getPreparedStatementCreatorFactory(ParsedSql parsedSql, SqlParameterSource paramSource) {
        String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
        List<SqlParameter> declaredParameters = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);
        return new PreparedStatementCreatorFactory(sqlToUse, declaredParameters);
    }
}
