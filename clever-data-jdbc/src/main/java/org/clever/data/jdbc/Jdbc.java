package org.clever.data.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.clever.core.Conv;
import org.clever.core.RenameStrategy;
import org.clever.core.codec.EncodeDecodeUtils;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.id.BusinessCodeUtils;
import org.clever.core.id.SnowFlake;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.IPage;
import org.clever.core.model.request.page.OrderItem;
import org.clever.core.model.request.page.Page;
import org.clever.core.tuples.TupleOne;
import org.clever.core.tuples.TupleThree;
import org.clever.core.tuples.TupleTwo;
import org.clever.dao.DataAccessException;
import org.clever.dao.DuplicateKeyException;
import org.clever.dao.support.DataAccessUtils;
import org.clever.data.AbstractDataSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.dialects.DialectFactory;
import org.clever.data.jdbc.listener.JdbcListeners;
import org.clever.data.jdbc.listener.OracleDbmsOutputListener;
import org.clever.data.jdbc.support.*;
import org.clever.jdbc.core.*;
import org.clever.jdbc.core.namedparam.EmptySqlParameterSource;
import org.clever.jdbc.core.namedparam.MapSqlParameterSource;
import org.clever.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.clever.jdbc.core.namedparam.SqlParameterSource;
import org.clever.jdbc.core.simple.SimpleJdbcCall;
import org.clever.jdbc.datasource.DataSourceTransactionManager;
import org.clever.jdbc.support.GeneratedKeyHolder;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.jdbc.support.KeyHolder;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionStatus;
import org.clever.transaction.annotation.Isolation;
import org.clever.transaction.annotation.Propagation;
import org.clever.transaction.support.DefaultTransactionDefinition;
import org.clever.transaction.support.TransactionCallback;
import org.clever.transaction.support.TransactionTemplate;
import org.clever.util.Assert;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Jdbc 数据库操作封装
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/08 20:55 <br/>
 */
@Slf4j
public class Jdbc extends AbstractDataSource {
    /**
     * <pre>{@code
     * 缓存调用存储过程调用对象(提高性能~20倍)
     * Map<dataSourceName@procedure_name, SimpleJdbcCall>
     * 参考: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/simple/SimpleJdbcCall.html
     * }</pre>
     */
    private final static ConcurrentMap<String, SimpleJdbcCall> PROCEDURE_CACHE = new ConcurrentHashMap<>();
    /**
     * 默认的返回数据字段名重命名策略
     */
    public static final RenameStrategy DEFAULT_RESULT_RENAME = RenameStrategy.None;
    /**
     * fields与whereMap字段名默认的重命名策略
     */
    public static final RenameStrategy DEFAULT_PARAMS_RENAME = RenameStrategy.None;
    /**
     * 事务默认是否只读
     */
    public static final boolean DEFAULT_READ_ONLY = false;
    /**
     * 分页时最大的页大小
     */
    public static final int PAGE_SIZE_MAX = QueryByPage.PAGE_SIZE_MAX;
    /**
     * 设置游标读取数据时，单批次的数据读取量(值不能太大也不能太小)
     */
    public static final int FETCH_SIZE = 500;
    /**
     * 事务默认超时时间
     */
    public static final int TX_TIMEOUT = 60;
    /**
     * 事务名称前缀
     */
    private static final String TRANSACTION_NAME_PREFIX = "TX";

    /**
     * 数据源名称
     */
    @Getter
    private final String dataSourceName;
    /**
     * 数据库类型
     */
    private final DbType dbType;
    /**
     * 数据源
     */
    private final DataSource dataSource;
    /**
     * JDBC API操作
     */
    @Getter
    private final NamedParameterJdbcTemplate jdbcTemplate;
    /**
     * 事务序列号
     */
    private final AtomicInteger transactionSerialNumber = new AtomicInteger(0);
    /**
     * 数据源管理器
     */
    @Getter
    private final DataSourceTransactionManager transactionManager;
    /**
     * 是否启用收集SQLWarning输出(支持Oracle的dbms_output输出)
     */
    private final ThreadLocal<Boolean> enableSqlWarning = new ThreadLocal<>();
    /**
     * 收集SQLWarning输出的数据缓冲区
     */
    private final ThreadLocal<StringBuilder> sqlWarningBuffer = new ThreadLocal<>();
    /**
     * 数据库操作监听器
     */
    private final JdbcListeners listeners = new JdbcListeners();

    /**
     * 使用Hikari连接池配置初始化数据源，创建对象
     *
     * @param hikariConfig Hikari连接池配置
     */
    public Jdbc(HikariConfig hikariConfig) {
        Assert.notNull(hikariConfig, "HikariConfig不能为空");
        this.dataSourceName = hikariConfig.getPoolName();
        this.dataSource = new HikariDataSource(hikariConfig);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(new JdbcTemplateWrapper(
                this.dataSource, this.enableSqlWarning, this.sqlWarningBuffer
        ));
        this.jdbcTemplate.getJdbcTemplate().setFetchSize(FETCH_SIZE);
        this.dbType = getDbType();
        this.transactionManager = new DataSourceTransactionManager(this.dataSource);
        initCheck();
        init();
    }

    /**
     * 使用DataSource创建对象
     *
     * @param dataSourceName 数据源名称
     * @param dataSource     数据源
     */
    public Jdbc(String dataSourceName, DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource不能为空");
        this.dataSourceName = dataSourceName;
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(new JdbcTemplateWrapper(
                this.dataSource, this.enableSqlWarning, this.sqlWarningBuffer
        ));
        this.jdbcTemplate.getJdbcTemplate().setFetchSize(FETCH_SIZE);
        this.dbType = getDbType();
        this.transactionManager = new DataSourceTransactionManager(this.dataSource);
        initCheck();
        init();
    }

    /**
     * 使用JdbcTemplate创建对象
     */
    public Jdbc(String dataSourceName, JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate不能为空");
        this.dataSourceName = dataSourceName;
        this.dataSource = jdbcTemplate.getDataSource();
        Assert.notNull(this.dataSource, "DataSource不能为空");
        this.jdbcTemplate = new NamedParameterJdbcTemplate(new JdbcTemplateWrapper(
                jdbcTemplate, this.enableSqlWarning, this.sqlWarningBuffer
        ));
        this.jdbcTemplate.getJdbcTemplate().setFetchSize(FETCH_SIZE);
        this.dbType = getDbType();
        this.transactionManager = new DataSourceTransactionManager(this.dataSource);
        initCheck();
        init();
    }

    /**
     * 使用JdbcTemplate创建对象
     */
    public Jdbc(String dataSourceName, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        Assert.notNull(namedParameterJdbcTemplate, "NamedParameterJdbcTemplate不能为空");
        this.dataSourceName = dataSourceName;
        this.dataSource = namedParameterJdbcTemplate.getJdbcTemplate().getDataSource();
        Assert.notNull(this.dataSource, "DataSource不能为空");
        this.jdbcTemplate = new NamedParameterJdbcTemplate(new JdbcTemplateWrapper(
                namedParameterJdbcTemplate.getJdbcTemplate(), this.enableSqlWarning, this.sqlWarningBuffer
        ));
        this.jdbcTemplate.getJdbcTemplate().setFetchSize(FETCH_SIZE);
        this.dbType = getDbType();
        this.transactionManager = new DataSourceTransactionManager(this.dataSource);
        initCheck();
        init();
    }

    /**
     * 获取数据库类型
     */
    public DbType getDbType() {
        if (this.dbType != null) {
            return this.dbType;
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return DbType.getDbTypeByUrl(connection.getMetaData().getURL());
        } catch (Throwable e) {
            throw new RuntimeException("读取数据库类型失败", e);
        } finally {
            if (connection != null) {
                JdbcUtils.closeConnection(connection);
            }
        }
    }

    /**
     * 校验数据源是否可用
     */
    @Override
    public void initCheck() {
        Assert.notNull(dbType, "DbType不能为空");
    }

    /**
     * 初始化操作
     */
    private void init() {
        listeners.add(new OracleDbmsOutputListener(this.enableSqlWarning, this.sqlWarningBuffer));
    }

    @Override
    public boolean isClosed() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            return hikariDataSource.isClosed();
        }
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            if (!hikariDataSource.isClosed()) {
                super.close();
                hikariDataSource.close();
            }
        } else {
            throw new UnsupportedOperationException("当前数据源不支持close");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Query 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<DbColumnMetaData> queryMetaData(String sql, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryData(sql, paramMap, new QueryMetaData<>(this, MetaDataResultSetExtractor.create(sql, resultRename)));
    }

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public List<DbColumnMetaData> queryMetaData(String sql, Map<String, Object> paramMap) {
        return queryMetaData(sql, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public List<DbColumnMetaData> queryMetaData(String sql) {
        return queryMetaData(sql, Collections.emptyMap(), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sql, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryData(sql, paramMap, new QueryOne<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Map<String, Object> queryOne(String sql, Map<String, Object> paramMap) {
        return queryOne(sql, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sql, RenameStrategy resultRename) {
        return queryOne(sql, Collections.emptyMap(), resultRename);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Map<String, Object> queryOne(String sql) {
        return queryOne(sql, Collections.emptyMap(), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sql, Object param, RenameStrategy resultRename) {
        return queryOne(sql, BeanCopyUtils.toMap(param), resultRename);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Map<String, Object> queryOne(String sql, Object param) {
        return queryOne(sql, BeanCopyUtils.toMap(param), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询一条数据，返回一个实体对象(sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     * @param clazz    查询对象类型
     */
    public <T> T queryOne(String sql, Map<String, Object> paramMap, Class<T> clazz) {
        return queryData(sql, paramMap, new QueryOne<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 查询一条数据，返回一个实体对象(sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> T queryOne(String sql, Class<T> clazz) {
        return queryOne(sql, Collections.emptyMap(), clazz);
    }

    /**
     * 查询一条数据，返回一个实体对象(sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> T queryOne(String sql, Object param, Class<T> clazz) {
        return queryOne(sql, BeanCopyUtils.toMap(param), clazz);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryFirst(String sql, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryData(sql, paramMap, new QueryOne<>(this, MapRowMapper.create(resultRename), true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Map<String, Object> queryFirst(String sql, Map<String, Object> paramMap) {
        return queryFirst(sql, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryFirst(String sql, RenameStrategy resultRename) {
        return queryFirst(sql, Collections.emptyMap(), resultRename);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Map<String, Object> queryFirst(String sql) {
        return queryFirst(sql, Collections.emptyMap(), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryFirst(String sql, Object param, RenameStrategy resultRename) {
        return queryFirst(sql, BeanCopyUtils.toMap(param), resultRename);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Map<String, Object> queryFirst(String sql, Object param) {
        return queryFirst(sql, BeanCopyUtils.toMap(param), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     * @param clazz    查询对象类型
     */
    public <T> T queryFirst(String sql, Map<String, Object> paramMap, Class<T> clazz) {
        return queryData(sql, paramMap, new QueryOne<>(this, new DataClassRowMapper<>(clazz), true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> T queryFirst(String sql, Class<T> clazz) {
        return queryFirst(sql, Collections.emptyMap(), clazz);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> T queryFirst(String sql, Object param, Class<T> clazz) {
        return queryFirst(sql, BeanCopyUtils.toMap(param), clazz);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryMany(String sql, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryData(sql, paramMap, new QueryMany<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public List<Map<String, Object>> queryMany(String sql, Map<String, Object> paramMap) {
        return queryMany(sql, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryMany(String sql, RenameStrategy resultRename) {
        return queryMany(sql, Collections.emptyMap(), resultRename);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public List<Map<String, Object>> queryMany(String sql) {
        return queryMany(sql, Collections.emptyMap(), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryMany(String sql, Object param, RenameStrategy resultRename) {
        return queryMany(sql, BeanCopyUtils.toMap(param), resultRename);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public List<Map<String, Object>> queryMany(String sql, Object param) {
        return queryMany(sql, BeanCopyUtils.toMap(param), DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     * @param clazz    查询对象类型
     */
    public <T> List<T> queryMany(String sql, Map<String, Object> paramMap, Class<T> clazz) {
        return queryData(sql, paramMap, new QueryMany<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryMany(String sql, Class<T> clazz) {
        return queryMany(sql, Collections.emptyMap(), clazz);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryMany(String sql, Object param, Class<T> clazz) {
        return queryMany(sql, BeanCopyUtils.toMap(param), clazz);
    }

    /**
     * 查询返回一个 String (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public String queryString(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, String.class));
    }

    /**
     * 查询返回一个 String (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public String queryString(String sql, Object param) {
        return queryString(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 String (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public String queryString(String sql) {
        return queryString(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 Long (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Long queryLong(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Long.class));
    }

    /**
     * 查询返回一个 Long (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Long queryLong(String sql, Object param) {
        return queryLong(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 Long (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Long queryLong(String sql) {
        return queryLong(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 Double (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Double queryDouble(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Double.class));
    }

    /**
     * 查询返回一个 Double (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Double queryDouble(String sql, Object param) {
        return queryDouble(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 Double (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Double queryDouble(String sql) {
        return queryDouble(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 BigDecimal (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public BigDecimal queryBigDecimal(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, BigDecimal.class));
    }

    /**
     * 查询返回一个 BigDecimal (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public BigDecimal queryBigDecimal(String sql, Object param) {
        return queryBigDecimal(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 BigDecimal (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public BigDecimal queryBigDecimal(String sql) {
        return queryBigDecimal(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 Boolean (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Boolean queryBoolean(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Boolean.class));
    }

    /**
     * 查询返回一个 Boolean (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Boolean queryBoolean(String sql, Object param) {
        return queryBoolean(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 Boolean (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Boolean queryBoolean(String sql) {
        return queryBoolean(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 Date (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Date queryDate(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Date.class));
    }

    /**
     * 查询返回一个 Date (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Date queryDate(String sql, Object param) {
        return queryDate(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 Date (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Date queryDate(String sql) {
        return queryDate(sql, Collections.emptyMap());
    }

    /**
     * 查询返回一个 Timestamp (sql返回多条数据会抛出异常)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Timestamp queryTimestamp(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Timestamp.class));
    }

    /**
     * 查询返回一个 Timestamp (sql返回多条数据会抛出异常)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Timestamp queryTimestamp(String sql, Object param) {
        return queryTimestamp(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询返回一个 Timestamp (sql返回多条数据会抛出异常)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Timestamp queryTimestamp(String sql) {
        return queryTimestamp(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public String queryFirstString(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, String.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public String queryFirstString(String sql, Object param) {
        return queryFirstString(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public String queryFirstString(String sql) {
        return queryFirstString(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Long queryFirstLong(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Long.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Long
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Long queryFirstLong(String sql, Object param) {
        return queryFirstLong(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Long
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Long queryFirstLong(String sql) {
        return queryFirstLong(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Double
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Double queryFirstDouble(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Double.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Double
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Double queryFirstDouble(String sql, Object param) {
        return queryFirstDouble(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Double
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Double queryFirstDouble(String sql) {
        return queryFirstDouble(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 BigDecimal
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public BigDecimal queryFirstBigDecimal(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, BigDecimal.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 BigDecimal
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public BigDecimal queryFirstBigDecimal(String sql, Object param) {
        return queryFirstBigDecimal(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 BigDecimal
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public BigDecimal queryFirstBigDecimal(String sql) {
        return queryFirstBigDecimal(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Boolean
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Boolean queryFirstBoolean(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Boolean.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Boolean
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Boolean queryFirstBoolean(String sql, Object param) {
        return queryFirstBoolean(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Boolean
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Boolean queryFirstBoolean(String sql) {
        return queryFirstBoolean(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Date
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Date queryFirstDate(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Date.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Date
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Date queryFirstDate(String sql, Object param) {
        return queryFirstDate(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Date
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Date queryFirstDate(String sql) {
        return queryFirstDate(sql, Collections.emptyMap());
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Timestamp
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public Timestamp queryFirstTimestamp(String sql, Map<String, Object> paramMap) {
        return queryData(sql, paramMap, new QueryObject<>(this, Timestamp.class, true));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Timestamp
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public Timestamp queryFirstTimestamp(String sql, Object param) {
        return queryFirstTimestamp(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Timestamp
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public Timestamp queryFirstTimestamp(String sql) {
        return queryFirstTimestamp(sql, Collections.emptyMap());
    }

    /**
     * SQL Count(获取一个SQL返回的数据总量)
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public long queryCount(String sql, Map<String, Object> paramMap) {
        Assert.hasText(sql, "sql不能为空");
        String countSql = SqlUtils.getCountSql(sql);
        countSql = StringUtils.trim(countSql);
        Long total = queryLong(countSql, paramMap);
        return Optional.ofNullable(total).orElse(0L);
    }

    /**
     * SQL Count(获取一个SQL返回的数据总量)
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public long queryCount(String sql, Object param) {
        return queryCount(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * SQL Count(获取一个SQL返回的数据总量)
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public long queryCount(String sql) {
        return queryCount(sql, Collections.emptyMap());
    }

    /**
     * 排序查询
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param sort         排序配置
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryDataBySort(sql, sort, paramMap, new QueryMany<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 排序查询
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param sort     排序配置
     * @param paramMap 参数，参数格式[:param]
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort, Map<String, Object> paramMap) {
        return queryBySort(sql, sort, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 排序查询
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param sort         排序配置
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort, RenameStrategy resultRename) {
        return queryBySort(sql, sort, Collections.emptyMap(), resultRename);
    }

    /**
     * 排序查询
     *
     * @param sql  sql脚本，参数格式[:param]
     * @param sort 排序配置
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort) {
        return queryBySort(sql, sort, Collections.emptyMap(), DEFAULT_RESULT_RENAME);
    }

    /**
     * 排序查询
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param sort         排序配置
     * @param param        参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort, Object param, RenameStrategy resultRename) {
        return queryBySort(sql, sort, BeanCopyUtils.toMap(param), resultRename);
    }

    /**
     * 排序查询
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param sort  排序配置
     * @param param 参数，参数格式[:param]
     */
    public List<Map<String, Object>> queryBySort(String sql, QueryBySort sort, Object param) {
        return queryBySort(sql, sort, BeanCopyUtils.toMap(param), DEFAULT_RESULT_RENAME);
    }

    /**
     * 排序查询
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param sort     排序配置
     * @param paramMap 参数，参数格式[:param]
     * @param clazz    查询对象类型
     */
    public <T> List<T> queryBySort(String sql, QueryBySort sort, Map<String, Object> paramMap, Class<T> clazz) {
        return queryDataBySort(sql, sort, paramMap, new QueryMany<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 排序查询
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param sort  排序配置
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryBySort(String sql, QueryBySort sort, Class<T> clazz) {
        return queryBySort(sql, sort, Collections.emptyMap(), clazz);
    }

    /**
     * 排序查询
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param sort  排序配置
     * @param param 参数，参数格式[:param]
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryBySort(String sql, QueryBySort sort, Object param, Class<T> clazz) {
        return queryBySort(sql, sort, BeanCopyUtils.toMap(param), clazz);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param pagination   分页配置(支持排序)
     * @param paramMap     参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination, Map<String, Object> paramMap, RenameStrategy resultRename) {
        return queryDataByPage(sql, pagination, paramMap, new QueryMany<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     * @param paramMap   参数，参数格式[:param]
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination, Map<String, Object> paramMap) {
        return queryByPage(sql, pagination, paramMap, DEFAULT_RESULT_RENAME);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param pagination   分页配置(支持排序)
     * @param resultRename 返回数据字段名重命名策略
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination, RenameStrategy resultRename) {
        return queryByPage(sql, pagination, new HashMap<>(2), resultRename);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination) {
        return queryByPage(sql, pagination, new HashMap<>(2), DEFAULT_RESULT_RENAME);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param pagination   分页配置(支持排序)
     * @param param        参数，参数格式[:param]
     * @param resultRename 返回数据字段名重命名策略
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination, Object param, RenameStrategy resultRename) {
        return queryByPage(sql, pagination, BeanCopyUtils.toMap(param), resultRename);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     * @param param      参数，参数格式[:param]
     */
    public IPage<Map<String, Object>> queryByPage(String sql, QueryByPage pagination, Object param) {
        return queryByPage(sql, pagination, BeanCopyUtils.toMap(param), DEFAULT_RESULT_RENAME);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     * @param paramMap   参数，参数格式[:param]
     * @param clazz      查询对象类型
     */
    public <T> IPage<T> queryByPage(String sql, QueryByPage pagination, Map<String, Object> paramMap, Class<T> clazz) {
        return queryDataByPage(sql, pagination, paramMap, new QueryMany<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     * @param clazz      查询对象类型
     */
    public <T> IPage<T> queryByPage(String sql, QueryByPage pagination, Class<T> clazz) {
        return queryByPage(sql, pagination, Collections.emptyMap(), clazz);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param pagination 分页配置(支持排序)
     * @param param      参数，参数格式[:param]
     * @param clazz      查询对象类型
     */
    public <T> IPage<T> queryByPage(String sql, QueryByPage pagination, Object param, Class<T> clazz) {
        return queryByPage(sql, pagination, BeanCopyUtils.toMap(param), clazz);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOneForTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename, RenameStrategy resultRename) {
        return queryDataForTable(tableName, whereMap, paramsRename, new QueryOne<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public Map<String, Object> queryOneForTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        return queryOneForTable(tableName, whereMap, paramsRename, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param whereMap  查询条件字段(只支持=，and条件)
     */
    public Map<String, Object> queryOneForTable(String tableName, Map<String, Object> whereMap) {
        return queryOneForTable(tableName, whereMap, DEFAULT_PARAMS_RENAME, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOneForTable(String tableName, Object where, RenameStrategy paramsRename, RenameStrategy resultRename) {
        return queryOneForTable(tableName, BeanCopyUtils.toMap(where), paramsRename, resultRename);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public Map<String, Object> queryOneForTable(String tableName, Object where, RenameStrategy paramsRename) {
        return queryOneForTable(tableName, BeanCopyUtils.toMap(where), paramsRename, DEFAULT_RESULT_RENAME);
    }


    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param where     查询条件字段(只支持=，and条件)
     */
    public Map<String, Object> queryOneForTable(String tableName, Object where) {
        return queryOneForTable(tableName, BeanCopyUtils.toMap(where), DEFAULT_PARAMS_RENAME, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param clazz        查询对象类型
     * @param paramsRename whereMap字段名重命名策略
     */
    public <T> T queryOneForTable(String tableName, Map<String, Object> whereMap, Class<T> clazz, RenameStrategy paramsRename) {
        return queryDataForTable(tableName, whereMap, paramsRename, new QueryOne<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param whereMap  查询条件字段(只支持=，and条件)
     * @param clazz     查询对象类型
     */
    public <T> T queryOneForTable(String tableName, Map<String, Object> whereMap, Class<T> clazz) {
        return queryOneForTable(tableName, whereMap, clazz, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param clazz        查询对象类型
     * @param paramsRename whereMap字段名重命名策略
     */
    public <T> T queryOneForTable(String tableName, Object where, Class<T> clazz, RenameStrategy paramsRename) {
        return queryOneForTable(tableName, BeanCopyUtils.toMap(where), clazz, paramsRename);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param where     查询条件字段(只支持=，and条件)
     * @param clazz     查询对象类型
     */
    public <T> T queryOneForTable(String tableName, Object where, Class<T> clazz) {
        return queryOneForTable(tableName, BeanCopyUtils.toMap(where), clazz, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename, RenameStrategy resultRename) {
        return queryDataForTable(tableName, whereMap, paramsRename, new QueryMany<>(this, MapRowMapper.create(resultRename)));
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        return queryManyForTable(tableName, whereMap, paramsRename, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param whereMap  查询条件字段(只支持=，and条件)
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Map<String, Object> whereMap) {
        return queryManyForTable(tableName, whereMap, DEFAULT_PARAMS_RENAME, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Object where, RenameStrategy paramsRename, RenameStrategy resultRename) {
        return queryManyForTable(tableName, BeanCopyUtils.toMap(where), paramsRename, resultRename);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Object where, RenameStrategy paramsRename) {
        return queryManyForTable(tableName, BeanCopyUtils.toMap(where), paramsRename, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param where     查询条件字段(只支持=，and条件)
     */
    public List<Map<String, Object>> queryManyForTable(String tableName, Object where) {
        return queryManyForTable(tableName, BeanCopyUtils.toMap(where), DEFAULT_PARAMS_RENAME, DEFAULT_RESULT_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param clazz        查询对象类型
     * @param paramsRename whereMap字段名重命名策略
     */
    public <T> List<T> queryManyForTable(String tableName, Map<String, Object> whereMap, Class<T> clazz, RenameStrategy paramsRename) {
        return queryDataForTable(tableName, whereMap, paramsRename, new QueryMany<>(this, new DataClassRowMapper<>(clazz)));
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param whereMap  查询条件字段(只支持=，and条件)
     * @param clazz     查询对象类型
     */
    public <T> List<T> queryManyForTable(String tableName, Map<String, Object> whereMap, Class<T> clazz) {
        return queryManyForTable(tableName, whereMap, clazz, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param where        查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public <T> List<T> queryManyForTable(String tableName, Object where, Class<T> clazz, RenameStrategy paramsRename) {
        return queryManyForTable(tableName, BeanCopyUtils.toMap(where), clazz, paramsRename);
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName 表名称
     * @param where     查询条件字段(只支持=，and条件)
     */
    public <T> List<T> queryManyForTable(String tableName, Object where, Class<T> clazz) {
        return queryManyForTable(tableName, BeanCopyUtils.toMap(where), clazz);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        final BatchDataReaderCallback batchDataReaderCallback = new BatchDataReaderCallback(batchSize, callback, resultRename);
        queryForCursor(sql, paramMap, new QueryForCursor(this, batchDataReaderCallback));
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param paramMap  参数，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param callback  游标批次读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, int batchSize, Function<BatchData, Boolean> callback) {
        queryForCursor(sql, paramMap, batchSize, callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        queryForCursor(sql, Collections.emptyMap(), batchSize, callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param callback  游标批次读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, int batchSize, Function<BatchData, Boolean> callback) {
        queryForCursor(sql, Collections.emptyMap(), batchSize, callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Object param, int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), batchSize, callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param param     参数，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param callback  游标批次读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, Object param, int batchSize, Function<BatchData, Boolean> callback) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), batchSize, callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param consumer     游标批次读取数据回调
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, paramMap, batchSize, batchData -> {
            consumer.accept(batchData);
            return false;
        }, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param paramMap  参数，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param consumer  游标批次读取数据消费者
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, int batchSize, Consumer<BatchData> consumer) {
        queryForCursor(sql, paramMap, batchSize, consumer, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, Collections.emptyMap(), batchSize, consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param consumer  游标批次读取数据消费者
     */
    public void queryForCursor(String sql, int batchSize, Consumer<BatchData> consumer) {
        queryForCursor(sql, Collections.emptyMap(), batchSize, consumer, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param batchSize    一个批次的数据量
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Object param, int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), batchSize, consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql       sql脚本，参数格式[:param]
     * @param param     参数，参数格式[:param]
     * @param batchSize 一个批次的数据量
     * @param consumer  游标批次读取数据消费者
     */
    public void queryForCursor(String sql, Object param, int batchSize, Consumer<BatchData> consumer) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), batchSize, consumer, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param callback     游标读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, Function<RowData, Boolean> callback, RenameStrategy resultRename) {
        final RowDataReaderCallback rowDataReaderCallback = new RowDataReaderCallback(callback, resultRename);
        queryForCursor(sql, paramMap, new QueryForCursor(this, rowDataReaderCallback));
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     * @param callback 游标读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, Function<RowData, Boolean> callback) {
        queryForCursor(sql, paramMap, callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param callback     游标读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Function<RowData, Boolean> callback, RenameStrategy resultRename) {
        queryForCursor(sql, Collections.emptyMap(), callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param callback 游标读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, Function<RowData, Boolean> callback) {
        queryForCursor(sql, Collections.emptyMap(), callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param callback     游标读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Object param, Function<RowData, Boolean> callback, RenameStrategy resultRename) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param param    参数，参数格式[:param]
     * @param callback 游标读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sql, Object param, Function<RowData, Boolean> callback) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), callback, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param paramMap     参数，参数格式[:param]
     * @param consumer     游标读取数据回调
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, Consumer<RowData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, paramMap, rowData -> {
            consumer.accept(rowData);
            return false;
        }, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     * @param consumer 游标读取数据回调
     */
    public void queryForCursor(String sql, Map<String, Object> paramMap, Consumer<RowData> consumer) {
        queryForCursor(sql, paramMap, consumer, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param consumer     游标读取数据回调
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Consumer<RowData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, Collections.emptyMap(), consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param consumer 游标读取数据回调
     */
    public void queryForCursor(String sql, Consumer<RowData> consumer) {
        queryForCursor(sql, Collections.emptyMap(), consumer, DEFAULT_RESULT_RENAME);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql          sql脚本，参数格式[:param]
     * @param param        参数，参数格式[:param]
     * @param consumer     游标读取数据回调
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sql, Object param, Consumer<RowData> consumer, RenameStrategy resultRename) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param param    参数，参数格式[:param]
     * @param consumer 游标读取数据回调
     */
    public void queryForCursor(String sql, Object param, Consumer<RowData> consumer) {
        queryForCursor(sql, BeanCopyUtils.toMap(param), consumer, DEFAULT_RESULT_RENAME);
    }

    // --------------------------------------------------------------------------------------------
    // Update 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public int update(String sql, Map<String, Object> paramMap) {
        return update(sql, paramMap, new UpdateData(this));
    }

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public int update(String sql, Object param) {
        return update(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public int update(String sql) {
        return update(sql, Collections.emptyMap());
    }

    /**
     * 根据表名更新表数据
     *
     * @param tableName    表名称
     * @param fields       更新字段值
     * @param whereMap     更新条件字段(只支持=，and条件)
     * @param paramsRename fields与whereMap字段名重命名策略
     */
    public int updateTable(String tableName, Map<String, Object> fields, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "更新表名称不能为空");
        Assert.notEmpty(fields, "更新字段不能为空");
        Assert.notEmpty(whereMap, "更新条件不能为空");
        tableName = StringUtils.trim(tableName);
        TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.updateSql(tableName, fields, whereMap, paramsRename);
        String sql = StringUtils.trim(tupleTow.getValue1());
        return update(sql, tupleTow.getValue2());
    }

    /**
     * 根据表名更新表数据
     *
     * @param tableName 表名称
     * @param fields    更新字段值
     * @param whereMap  更新条件字段(只支持=，and条件)
     */
    public int updateTable(String tableName, Map<String, Object> fields, Map<String, Object> whereMap) {
        return updateTable(tableName, fields, whereMap, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 根据表名更新表数据
     *
     * @param tableName    表名称
     * @param fields       更新字段值
     * @param where        更新条件字段(只支持=，and条件)
     * @param paramsRename fields与whereMap字段名重命名策略
     */
    public int updateTable(String tableName, Object fields, Object where, RenameStrategy paramsRename) {
        return updateTable(tableName, BeanCopyUtils.toMap(fields), BeanCopyUtils.toMap(where), paramsRename);
    }

    /**
     * 根据表名更新表数据
     *
     * @param tableName 表名称
     * @param fields    更新字段值
     * @param where     更新条件字段(只支持=，and条件)
     */
    public int updateTable(String tableName, Object fields, Object where) {
        return updateTable(tableName, BeanCopyUtils.toMap(fields), BeanCopyUtils.toMap(where), DEFAULT_PARAMS_RENAME);
    }

    /**
     * 更新数据库表数据
     *
     * @param tableName    表名称
     * @param fields       更新字段值
     * @param whereStr     自定义where条件(不用写where关键字)
     * @param paramsRename fields与whereMap字段名重命名策略
     */
    public int updateTable(String tableName, Map<String, Object> fields, String whereStr, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "更新表名称不能为空");
        Assert.notEmpty(fields, "更新字段不能为空");
        Assert.hasText(whereStr, "更新条件不能为空");
        tableName = StringUtils.trim(tableName);
        TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.updateSql(tableName, fields, null, paramsRename);
        String sql = String.format("%s where %s", tupleTow.getValue1(), StringUtils.trim(whereStr));
        return update(sql, tupleTow.getValue2());
    }

    /**
     * 更新数据库表数据
     *
     * @param tableName 表名称
     * @param fields    更新字段值
     * @param whereStr  自定义where条件(不用写where关键字)
     */
    public int updateTable(String tableName, Map<String, Object> fields, String whereStr) {
        return updateTable(tableName, fields, whereStr, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 更新数据库表数据
     *
     * @param tableName    表名称
     * @param fields       更新字段值
     * @param whereStr     自定义where条件(不用写where关键字)
     * @param paramsRename fields与whereMap字段名重命名策略
     */
    public int updateTable(String tableName, Object fields, String whereStr, RenameStrategy paramsRename) {
        return updateTable(tableName, BeanCopyUtils.toMap(fields), whereStr, paramsRename);
    }

    /**
     * 更新数据库表数据
     *
     * @param tableName 表名称
     * @param fields    更新字段值
     * @param whereStr  自定义where条件(不用写where关键字)
     */
    public int updateTable(String tableName, Object fields, String whereStr) {
        return updateTable(tableName, BeanCopyUtils.toMap(fields), whereStr, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 批量执行更新SQL，返回更新影响数据量
     *
     * @param sql    sql脚本，参数格式[:param]
     * @param params 参数集合，参数格式[:param]
     */
    public int[] batchUpdate(String sql, List<?> params) {
        return batchUpdate(sql, params, new BatchUpdateData(this));
    }

    // --------------------------------------------------------------------------------------------
    // Delete 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 删除数据库表数据
     *
     * @param tableName    表名称
     * @param whereMap     更新条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public int deleteTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "删除表名称不能为空");
        Assert.notEmpty(whereMap, "删除条件不能为空");
        tableName = StringUtils.trim(tableName);
        TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.deleteSql(tableName, whereMap, paramsRename);
        String sql = StringUtils.trim(tupleTow.getValue1());
        return update(sql, tupleTow.getValue2());
    }

    /**
     * 删除数据库表数据
     *
     * @param tableName 表名称
     * @param whereMap  更新条件字段(只支持=，and条件)
     */
    public int deleteTable(String tableName, Map<String, Object> whereMap) {
        return deleteTable(tableName, whereMap, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 删除数据库表数据
     *
     * @param tableName    表名称
     * @param where        更新条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     */
    public int deleteTable(String tableName, Object where, RenameStrategy paramsRename) {
        return deleteTable(tableName, BeanCopyUtils.toMap(where), paramsRename);
    }

    /**
     * 删除数据库表数据
     *
     * @param tableName 表名称
     * @param where     更新条件字段(只支持=，and条件)
     */
    public int deleteTable(String tableName, Object where) {
        return deleteTable(tableName, BeanCopyUtils.toMap(where), DEFAULT_PARAMS_RENAME);
    }

    // --------------------------------------------------------------------------------------------
    // Insert 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sql      sql脚本，参数格式[:param]
     * @param paramMap 参数，参数格式[:param]
     */
    public InsertResult insert(String sql, Map<String, Object> paramMap) {
        return insert(sql, paramMap, new InsertData(this));
    }

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sql   sql脚本，参数格式[:param]
     * @param param 参数，参数格式[:param]
     */
    public InsertResult insert(String sql, Object param) {
        return insert(sql, BeanCopyUtils.toMap(param));
    }

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sql sql脚本，参数格式[:param]
     */
    public InsertResult insert(String sql) {
        return insert(sql, null);
    }

    /**
     * 数据插入到表
     *
     * @param tableName    表名称
     * @param fields       字段名
     * @param paramsRename fields字段名重命名策略
     */
    public InsertResult insertTable(String tableName, Map<String, Object> fields, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "插入表名称不能为空");
        Assert.notEmpty(fields, "插入字段不能为空");
        tableName = StringUtils.trim(tableName);
        TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.insertSql(tableName, fields, paramsRename);
        return insert(tupleTow.getValue1(), tupleTow.getValue2());
    }

    /**
     * 数据插入到表
     *
     * @param tableName 表名称
     * @param fields    字段名
     */
    public InsertResult insertTable(String tableName, Map<String, Object> fields) {
        return insertTable(tableName, fields, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 数据插入到表
     *
     * @param tableName    表名称
     * @param fields       字段名
     * @param paramsRename fields字段名重命名策略
     */
    public InsertResult insertTable(String tableName, Object fields, RenameStrategy paramsRename) {
        return insertTable(tableName, BeanCopyUtils.toMap(fields), paramsRename);
    }

    /**
     * 数据插入到表
     *
     * @param tableName 表名称
     * @param fields    字段名
     */
    public InsertResult insertTable(String tableName, Object fields) {
        return insertTable(tableName, BeanCopyUtils.toMap(fields), DEFAULT_PARAMS_RENAME);
    }

    /**
     * 数据插入到表
     *
     * @param tableName    表名称
     * @param fieldsList   字段名集合
     * @param paramsRename fields字段名重命名策略
     */
    public List<InsertResult> insertTable(String tableName, List<?> fieldsList, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "插入表名称不能为空");
        Assert.notEmpty(fieldsList, "插入字段不能为空");
        tableName = StringUtils.trim(tableName);
        List<InsertResult> resultList = new ArrayList<>(fieldsList.size());
        for (Object fields : fieldsList) {
            TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.insertSql(tableName, BeanCopyUtils.toMap(fields), paramsRename);
            InsertResult insertResult = insert(tupleTow.getValue1(), tupleTow.getValue2());
            resultList.add(insertResult);
        }
        return resultList;
    }

    /**
     * 数据插入到表
     *
     * @param tableName  表名称
     * @param fieldsList 字段名集合
     */
    public List<InsertResult> insertTable(String tableName, List<?> fieldsList) {
        return insertTable(tableName, fieldsList, DEFAULT_PARAMS_RENAME);
    }

    /**
     * 数据批量插入到表
     *
     * @param tableName    表名称
     * @param fieldsList   字段名集合
     * @param paramsRename fields字段名重命名策略
     */
    public int batchInsertTable(String tableName, List<?> fieldsList, RenameStrategy paramsRename) {
        Assert.hasText(tableName, "插入表名称不能为空");
        Assert.notEmpty(fieldsList, "插入字段不能为空");
        tableName = StringUtils.trim(tableName);
        Map<String, List<Map<String, Object>>> sqlMap = new HashMap<>();
        for (Object fields : fieldsList) {
            TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.insertSql(tableName, BeanCopyUtils.toMap(fields), paramsRename);
            sqlMap.computeIfAbsent(tupleTow.getValue1(), sql -> new ArrayList<>()).add(tupleTow.getValue2());
        }
        int sum = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : sqlMap.entrySet()) {
            int[] arr = batchUpdate(entry.getKey(), entry.getValue(), new BatchUpdateData(this));
            for (int count : arr) {
                // java.sql.Statement#SUCCESS_NO_INFO
                if (count < 0) {
                    return arr.length;
                }
                sum += count;
            }
        }
        return sum;
    }

    /**
     * 数据批量插入到表
     *
     * @param tableName  表名称
     * @param fieldsList 字段名集合
     */
    public int batchInsertTable(String tableName, List<?> fieldsList) {
        return batchInsertTable(tableName, fieldsList, DEFAULT_PARAMS_RENAME);
    }

    // --------------------------------------------------------------------------------------------
    //  调用存储过程
    // --------------------------------------------------------------------------------------------

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param paramMap      参数
     */
    public Map<String, Object> callGet(final String procedureName, Map<String, ?> paramMap) {
        SimpleJdbcCall jdbcCall = getJdbcCall(procedureName);
        MapSqlParameterSource sqlParameter = new MapSqlParameterSource(paramMap);
        return jdbcCall.execute(sqlParameter);
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public Map<String, Object> callGet(final String procedureName, Object... params) {
        SimpleJdbcCall jdbcCall = getJdbcCall(procedureName);
        return jdbcCall.execute(params);
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public Map<String, Object> callGet(String procedureName, List<?> params) {
        return callGet(procedureName, params.toArray());
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     */
    public Map<String, Object> callGet(String procedureName) {
        return callGet(procedureName, new HashMap<>());
    }

    /**
     * 执行存储过程
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public void call(String procedureName, Object... params) {
        TupleTwo<String, Map<String, Object>> sqlInfo = SqlUtils.getCallSql(procedureName, Arrays.asList(params));
        update(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 执行存储过程
     *
     * @param procedureName 存贮过程名称
     */
    public void call(String procedureName) {
        call(procedureName, Collections.emptyList());
    }

    // --------------------------------------------------------------------------------------------
    //  事务操作
    // --------------------------------------------------------------------------------------------

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly            设置事务是否只读
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel, boolean readOnly) {
        Assert.notNull(action, "数据库操作不能为空");
        TransactionTemplate transactionTemplate = createTransactionDefinition(isolationLevel, propagationBehavior, readOnly, timeout);
        return transactionTemplate.execute(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly    设置事务是否只读
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, Propagation propagation, int timeout, Isolation isolation, boolean readOnly) {
        return beginTX(action, propagation.value(), timeout, isolation.value(), readOnly);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly            设置事务是否只读
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, int propagationBehavior, int timeout, int isolationLevel, boolean readOnly) {
        beginTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior, timeout, isolationLevel, readOnly);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly    设置事务是否只读
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, Propagation propagation, int timeout, Isolation isolation, boolean readOnly) {
        beginTX(action, propagation.value(), timeout, isolation.value(), readOnly);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return beginTX(action, propagationBehavior, timeout, isolationLevel, DEFAULT_READ_ONLY);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, Propagation propagation, int timeout, Isolation isolation) {
        return beginTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, int propagationBehavior, int timeout, int isolationLevel) {
        beginTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior, timeout, isolationLevel);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, Propagation propagation, int timeout, Isolation isolation) {
        beginTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return beginTX(action, propagationBehavior, timeout, TransactionDefinition.ISOLATION_DEFAULT, DEFAULT_READ_ONLY);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间(单位：秒)
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, Propagation propagation, int timeout) {
        return beginTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, int propagationBehavior, int timeout) {
        beginTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior, timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间(单位：秒)
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, Propagation propagation, int timeout) {
        beginTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior) {
        return beginTX(action, propagationBehavior, TX_TIMEOUT, TransactionDefinition.ISOLATION_DEFAULT, DEFAULT_READ_ONLY);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, Propagation propagation) {
        return beginTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, int propagationBehavior) {
        beginTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, Propagation propagation) {
        beginTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action) {
        return beginTX(action, TransactionDefinition.PROPAGATION_REQUIRED, TX_TIMEOUT, TransactionDefinition.ISOLATION_DEFAULT, DEFAULT_READ_ONLY);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action) {
        beginTX(status -> {
            action.accept(status);
            return null;
        });
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return beginTX(action, propagationBehavior, timeout, isolationLevel, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, Propagation propagation, int timeout, Isolation isolation) {
        return beginReadOnlyTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, int propagationBehavior, int timeout, int isolationLevel) {
        beginReadOnlyTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior, timeout, isolationLevel);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolation   设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, Propagation propagation, int timeout, Isolation isolation) {
        beginReadOnlyTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return beginTX(action, propagationBehavior, timeout, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, Propagation propagation, int timeout) {
        return beginReadOnlyTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, int propagationBehavior, int timeout) {
        beginReadOnlyTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior, timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout     设置事务超时时间，-1表示不超时(单位：秒)
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, Propagation propagation, int timeout) {
        beginReadOnlyTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior) {
        return beginTX(action, propagationBehavior, TX_TIMEOUT, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>         返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, Propagation propagation) {
        return beginReadOnlyTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, int propagationBehavior) {
        beginReadOnlyTX(status -> {
            action.accept(status);
            return null;
        }, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, Propagation propagation) {
        beginReadOnlyTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return beginTX(action, TransactionDefinition.PROPAGATION_REQUIRED, TX_TIMEOUT, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action) {
        beginReadOnlyTX(status -> {
            action.accept(status);
            return null;
        });
    }

    // --------------------------------------------------------------------------------------------
    //  其它 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取SQLWarning输出(支持Oracle的dbms_output输出)
     *
     * @param clear 是否清空SQLWarning缓存
     */
    public String getSqlWarning(boolean clear) {
        StringBuilder output = this.sqlWarningBuffer.get();
        if (clear) {
            this.sqlWarningBuffer.remove();
        }
        return output == null ? "" : output.toString();
    }

    /**
     * 获取SQLWarning输出(支持Oracle的dbms_output输出)
     */
    public String getSqlWarning() {
        return getSqlWarning(false);
    }

    /**
     * 启用收集SQLWarning输出(支持Oracle的dbms_output输出)
     */
    public void enableSqlWarning() {
        this.enableSqlWarning.set(true);
    }

    /**
     * 禁用收集SQLWarning输出(支持Oracle的dbms_output输出)
     *
     * @return 返回之前输出的数据 & 清空数据
     */
    public String disableSqlWarning() {
        this.enableSqlWarning.remove();
        return getSqlWarning(true);
    }

    /**
     * 获取数据源信息
     */
    public JdbcInfo getInfo() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            JdbcInfo jdbcInfo = new JdbcInfo();
            jdbcInfo.setDriverClassName(hikariDataSource.getDriverClassName());
            jdbcInfo.setJdbcUrl(hikariDataSource.getJdbcUrl());
            jdbcInfo.setAutoCommit(hikariDataSource.isAutoCommit());
            jdbcInfo.setReadOnly(hikariDataSource.isReadOnly());
            jdbcInfo.setDbType(dbType);
            jdbcInfo.setClosed(hikariDataSource.isClosed());
            return jdbcInfo;
        } else {
            throw new UnsupportedOperationException("当前数据源类型：" + dataSource.getClass().getName() + "，不支持此操作");
        }
    }

    /**
     * 获取数据源状态
     */
    public JdbcDataSourceStatus getStatus() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            JdbcDataSourceStatus status = new JdbcDataSourceStatus();
            status.setTotalConnections(poolMXBean.getTotalConnections());
            status.setActiveConnections(poolMXBean.getActiveConnections());
            status.setIdleConnections(poolMXBean.getIdleConnections());
            status.setThreadsAwaitingConnection(poolMXBean.getThreadsAwaitingConnection());
            return status;
        } else {
            throw new UnsupportedOperationException("当前数据源类型：" + dataSource.getClass().getName() + "，不支持此操作");
        }
    }

    // --------------------------------------------------------------------------------------------
    //  业务含义操作
    // --------------------------------------------------------------------------------------------

    /**
     * 返回下一个主键值
     *
     * @param primaryKeyName 主键名称
     */
    public Long nextPK(String primaryKeyName) {
        TupleTwo<String, Map<String, Object>> sqlInfo = DialectFactory.nextPKSql(primaryKeyName, dbType, null);
        Assert.notNull(sqlInfo, "sqlInfo 不能为空");
        Assert.isNotBlank(sqlInfo.getValue1(), "sqlInfo.sql 不能为空");
        Assert.notNull(sqlInfo.getValue2(), "sqlInfo.params 不能为空");
        return beginTX(status -> {
            return queryLong(sqlInfo.getValue1(), sqlInfo.getValue2());
        });
    }

    /***
     * 批量获取唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     * @param size 唯一id值数量(1 ~ 10W)
     */
    public List<Long> nextIds(String idName, int size) {
        Assert.isTrue(size >= 1 && size <= 10_0000, "size取值范围必须在1 ~ 10W之间");
        final Map<String, Object> params = new HashMap<>();
        params.put("sequence_name", idName);
        final TupleOne<Long> rowId = TupleOne.creat(queryLong("select id from auto_increment_id where sequence_name=:sequence_name", params));
        Long currentValue = beginTX(status -> {
            if (rowId.getValue1() == null) {
                try {
                    // 在一个新事物里新增数据(尽可能让其他事务能使用这条数据) 也可以避免postgresql的on_error_rollback问题
                    beginTX(innerStatus -> {
                        Long id = SnowFlake.SNOW_FLAKE.nextId();
                        params.clear();
                        params.put("id", id);
                        params.put("sequence_name", idName);
                        params.put("description", "系统自动生成");
                        params.put("create_at", new Date());
                        insertTable("auto_increment_id", params, RenameStrategy.None);
                    }, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                } catch (DuplicateKeyException e) {
                    // 插入数据失败: 唯一约束错误
                    log.warn("插入 auto_increment_id 表失败: {}", e.getMessage());
                } catch (Exception e) {
                    log.warn("插入 auto_increment_id 表失败", e);
                }
                // 等待数据插入成功
                final int maxRetryCount = 128;
                params.clear();
                params.put("sequence_name", idName);
                for (int i = 0; i < maxRetryCount; i++) {
                    rowId.setValue1(queryLong("select id from auto_increment_id where sequence_name=:sequence_name", params));
                    if (rowId.getValue1() != null) {
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            if (rowId.getValue1() == null) {
                throw new RuntimeException("auto_increment_id 表数据不存在(未知的异常)");
            }
            // 更新序列数据(使用数据库行级锁保证并发性)
            params.clear();
            params.put("size", size);
            params.put("id", rowId.getValue1());
            params.put("update_at", new Date());
            update("update auto_increment_id set current_value=current_value+:size, update_at=:update_at where id=:id", params);
            // 查询更新之后的值
            params.clear();
            params.put("id", rowId.getValue1());
            return queryLong("select current_value from auto_increment_id where id=:id", params);
        }, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        long oldValue = currentValue - size;
        List<Long> ids = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            ids.add(oldValue + i);
        }
        return ids;
    }

    /**
     * 返回下一个唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     */
    public Long nextId(String idName) {
        List<Long> ids = nextIds(idName, 1);
        return ids.get(0);
    }

    /**
     * 返回当前唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     */
    public Long currentId(String idName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("sequence_name", idName);
        return beginTX(status -> {
            Long id = queryLong("select current_value from auto_increment_id where sequence_name=:sequence_name", params);
            return id == null ? -1L : id;
        }, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 批量获取唯一的 code 值 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>
     * 支持: ${date_format_pattern}、${seq_size}、${id_size}，例如：
     * CK${yyMMddHHmm}${seq}    -> CK22120108301、CK221201083023
     * CK${yyyyMMdd}_${seq3}    -> CK20221201_001、CK20221201_023
     * CK${yy}-${MMdd}-${seq3}  -> CK22-1201-001、CK22-1201-023
     * </pre>
     *
     * @param codeName code名称
     * @param size     唯一 code 值数量(1 ~ 10W)
     */
    public List<String> nextCodes(String codeName, int size) {
        Assert.isTrue(size >= 1 && size <= 10_0000, "size取值范围必须在1 ~ 10W之间");
        TupleThree<String, Date, Long> res = beginTX(status -> {
            // 使用数据库行级锁保证并发性
            Map<String, Object> params = new HashMap<>();
            params.put("code_name", codeName);
            Map<String, Object> result = queryOne(
                    "select id, pattern, sequence, reset_pattern, reset_flag from biz_code where code_name=:code_name for update",
                    params,
                    RenameStrategy.None
            );
            if (result == null) {
                throw new RuntimeException("biz_code 表数据不存在: code_name=" + codeName);
            }
            final Long rowId = Conv.asLong(result.get("id"));
            final String pattern = Conv.asString(result.get("pattern"));
            final Long sequence = Conv.asLong(result.get("sequence"));
            final String resetPattern = Conv.asString(result.get("reset_pattern"));
            final String resetFlag = Conv.asString(result.get("reset_flag"));
            final Date now = new Date();
            // 计算 reset_flag
            String newResetFlag = resetFlag;
            Long newSequence = sequence;
            if (StringUtils.isNotBlank(resetPattern)) {
                newResetFlag = DateFormatUtils.format(now, resetPattern);
            }
            // 判断是否需要重置 sequence 计数
            if (!Objects.equals(resetFlag, newResetFlag)) {
                newSequence = 0L;
            }
            newSequence = newSequence + size;
            // 更新数据库值
            params.clear();
            params.put("id", rowId);
            Map<String, Object> fields = new HashMap<>();
            fields.put("sequence", newSequence);
            fields.put("reset_flag", newResetFlag);
            fields.put("update_at", now);
            updateTable("biz_code", fields, params, RenameStrategy.None);
            return TupleThree.creat(pattern, now, newSequence);
        }, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        long oldValue = res.getValue3() - size;
        List<String> codes = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            codes.add(BusinessCodeUtils.create(res.getValue1(), res.getValue2(), oldValue + i));
        }
        return codes;
    }

    /**
     * 批量获取唯一的 code 值 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>
     * 支持: ${date_format_pattern}、${seq_size}、${id_size}，例如：
     * CK${yyMMddHHmm}${seq}    -> CK22120108301、CK221201083023
     * CK${yyyyMMdd}_${seq3}    -> CK20221201_001、CK20221201_023
     * CK${yy}-${MMdd}-${seq3}  -> CK22-1201-001、CK22-1201-023
     * </pre>
     *
     * @param codeName code名称
     */
    public String nextCode(String codeName) {
        List<String> codes = nextCodes(codeName, 1);
        return codes.get(0);
    }

    /**
     * 批量获取唯一的 code 值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param codeName code名称
     */
    public String currentCode(String codeName) {
        Map<String, Object> params = new HashMap<>();
        params.put("code_name", codeName);
        return beginTX(status -> {
            Map<String, Object> result = queryOne("select pattern, sequence from biz_code where code_name=:code_name", params, RenameStrategy.None);
            String code = null;
            if (result != null) {
                code = BusinessCodeUtils.create(Conv.asString(result.get("pattern")), new Date(), Conv.asLong(result.get("sequence")));
            }
            return code;
        }, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 借助数据库表实现的排他锁 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>{@code
     *   lock("lockName", () -> {
     *      // 同步业务逻辑处理...
     *      return result;
     *   })
     * }</pre>
     *
     * @param lockName  锁名称
     * @param syncBlock 同步代码块
     */
    @SneakyThrows
    public <T> T lock(String lockName, Supplier<T> syncBlock) {
        Assert.isNotBlank(lockName, "参数 lockName 不能为空");
        Assert.notNull(syncBlock, "参数 syncBlock 不能为空");
        final JdbcOperations jdbcOperations = jdbcTemplate.getJdbcOperations();
        return jdbcOperations.execute((ConnectionCallback<T>) connection -> {
            final String lockSql = "UPDATE sys_lock SET lock_count=lock_count+1, update_at=? WHERE lock_name=?";
            final String insertSql = "INSERT INTO sys_lock (id, lock_name, lock_count, description, create_at) VALUES (?, ?, ?, ?, ?)";
            final String selectSql = "SELECT id FROM sys_lock WHERE lock_name=?";
            // 备份连接状态
            int transactionIsolation = connection.getTransactionIsolation();
            boolean autoCommit = connection.getAutoCommit();
            try {
                // 这里使用新的连接获取数据库行级锁
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                // 使用数据库行级锁保证并发性
                int lock = executeUpdate(connection, lockSql, new Object[]{new Date(), lockName});
                // 锁数据不存在就创建锁数据
                if (lock <= 0) {
                    try {
                        // 在一个新事物里新增锁数据(尽可能让其他事务能使用这个锁)
                        jdbcOperations.execute((ConnectionCallback<Integer>) innerCon -> {
                            long id = SnowFlake.SNOW_FLAKE.nextId();
                            int change = executeUpdate(innerCon, insertSql, new Object[]{id, lockName, 0, "系统自动生成", new Date()});
                            innerCon.commit();
                            return change;
                        });
                    } catch (DuplicateKeyException e) {
                        // 插入数据失败: 唯一约束错误
                        log.warn("插入 sys_lock 表失败: {}", e.getMessage());
                    } catch (DataAccessException e) {
                        log.warn("插入 sys_lock 表失败", e);
                    }
                    // 等待锁数据插入完成
                    final int maxRetryCount = 128;
                    for (int i = 0; i < maxRetryCount; i++) {
                        List<Map<String, Object>> list = executeQuery(connection, selectSql, new Object[]{lockName});
                        if (list != null && !list.isEmpty()) {
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    // 使用数据库行级锁保证并发性
                    lock = executeUpdate(connection, lockSql, new Object[]{new Date(), lockName});
                    if (lock <= 0) {
                        throw new RuntimeException("sys_lock 表数据不存在(未知的异常)");
                    }
                }
                // 执行同步代码块
                T result = syncBlock.get();
                connection.commit();
                return result;
            } catch (Exception e) {
                // 异常回滚事务
                connection.rollback();
                throw ExceptionUtils.unchecked(e);
            } finally {
                // 还原连接状态后关闭连接
                connection.setTransactionIsolation(transactionIsolation);
                connection.setAutoCommit(autoCommit);
                connection.close();
            }
        });
    }

    /**
     * 借助数据库表实现的排他锁 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>{@code
     *   lock("lockName", () -> {
     *      // 同步业务逻辑处理...
     *   })
     * }</pre>
     *
     * @param lockName  锁名称
     * @param syncBlock 同步代码块
     */
    @SneakyThrows
    public void lock(String lockName, Runnable syncBlock) {
        lock(lockName, () -> {
            syncBlock.run();
            return null;
        });
    }

    // --------------------------------------------------------------------------------------------
    //  内部函数
    // --------------------------------------------------------------------------------------------

    JdbcListeners getListeners() {
        return listeners;
    }

    /**
     * 查询一条数据
     *
     * @param sql         sql脚本，参数格式[:param]
     * @param paramMap    参数，参数格式[:param]
     * @param jdbcExecute 数据查询逻辑
     */
    private <T> T queryData(String sql, Map<String, Object> paramMap, JdbcExecute<T> jdbcExecute) {
        Assert.hasText(sql, "sql不能为空");
        sql = StringUtils.trim(sql);
        T res = jdbcExecute.execute(new JdbcContext(sql, paramMap));
        SqlLoggerUtils.printfTotal(res);
        return res;
    }

    /**
     * 排序查询
     *
     * @param sql         sql脚本，参数格式[:param]
     * @param sort        排序配置
     * @param paramMap    参数，参数格式[:param]
     * @param jdbcExecute 数据查询逻辑
     */
    private <T> T queryDataBySort(String sql, QueryBySort sort, Map<String, Object> paramMap, JdbcExecute<T> jdbcExecute) {
        // 构造排序以及分页sql
        sql = StringUtils.trim(sql);
        String sortSql = SqlUtils.concatOrderBy(sql, sort);
        return queryData(sortSql, paramMap, jdbcExecute);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sql         sql脚本，参数格式[:param]
     * @param pagination  分页配置(支持排序)
     * @param paramMap    参数，参数格式[:param]
     * @param jdbcExecute 数据查询逻辑
     */
    private <T> IPage<T> queryDataByPage(String sql, QueryByPage pagination, Map<String, Object> paramMap, JdbcExecute<List<T>> jdbcExecute) {
        Assert.hasText(sql, "sql不能为空");
        Assert.notNull(pagination, "分页配置不能为空");
        sql = StringUtils.trim(sql);
        Page<T> page = new Page<>(pagination.getPageNo(), Math.min(pagination.getPageSize(), PAGE_SIZE_MAX));
        // 执行 count 查询
        if (pagination.isSearchCount()) {
            long total = queryCount(sql, paramMap);
            page.setTotal(total);
            // 溢出总页数，设置最后一页
            long pages = page.getPages();
            if (page.getCurrent() > pages) {
                page.setCurrent(pages);
            }
        } else {
            page.setSearchCount(false);
            page.setTotal(-1);
        }
        // 构造排序以及分页sql
        String sortSql = SqlUtils.concatOrderBy(sql, pagination);
        page.setExportDataSql(sortSql);
        page.setExportDataSqlParams(paramMap);
        if (this.dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) this.dataSource;
            page.getDbInfo().put("dbType", dbType);
            page.getDbInfo().put("jdbcurl", EncodeDecodeUtils.encodeHex(hikariDataSource.getJdbcUrl().getBytes(StandardCharsets.UTF_8)));
            page.getDbInfo().put("username", EncodeDecodeUtils.encodeHex(hikariDataSource.getUsername().getBytes(StandardCharsets.UTF_8)));
            page.getDbInfo().put("password", EncodeDecodeUtils.encodeHex(hikariDataSource.getPassword().getBytes(StandardCharsets.UTF_8)));
        }
        String pageSql = DialectFactory.buildPaginationSql(page, sortSql, paramMap, dbType, null);
        // 执行 pageSql
        List<T> listData = queryData(pageSql, paramMap, jdbcExecute);
        page.setRecords(listData);
        // 排序信息
        List<String> orderFieldsTmp = pagination.getOrderFields();
        List<String> sortsTmp = pagination.getSorts();
        for (int i = 0; i < orderFieldsTmp.size(); i++) {
            String fieldSql = orderFieldsTmp.get(i);
            String sort = sortsTmp.get(i);
            OrderItem orderItem = new OrderItem();
            orderItem.setColumn(fieldSql);
            orderItem.setAsc(SqlUtils.ASC.equalsIgnoreCase(StringUtils.trim(sort)));
            page.addOrder(orderItem);
        }
        return page;
    }

    /**
     * 根据表名查询数据
     *
     * @param tableName    表名称
     * @param whereMap     查询条件字段(只支持=，and条件)
     * @param paramsRename whereMap字段名重命名策略
     * @param jdbcExecute  数据查询逻辑
     */
    private <T> T queryDataForTable(String tableName, Map<String, Object> whereMap, RenameStrategy paramsRename, JdbcExecute<T> jdbcExecute) {
        Assert.hasText(tableName, "查询表名称不能为空");
        Assert.notEmpty(whereMap, "查询条件不能为空");
        TupleTwo<String, Map<String, Object>> tupleTow = SqlUtils.selectSql(tableName, whereMap, paramsRename);
        String sql = StringUtils.trim(tupleTow.getValue1());
        return queryData(sql, tupleTow.getValue2(), jdbcExecute);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sql            sql脚本，参数格式[:param]
     * @param paramMap       参数，参数格式[:param]
     * @param queryForCursor 数据查询逻辑
     */
    private void queryForCursor(String sql, Map<String, Object> paramMap, QueryForCursor queryForCursor) {
        Assert.hasText(sql, "sql不能为空");
        Assert.notNull(queryForCursor, "queryForCursor不能为空");
        sql = StringUtils.trim(sql);
        if (paramMap == null) {
            paramMap = new HashMap<>();
        }
        queryForCursor.execute(new JdbcContext(sql, paramMap));
        SqlLoggerUtils.printfTotal(queryForCursor.getInterruptRowCallbackHandler().getRowCount());
    }

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param paramMap   参数，参数格式[:param]
     * @param updateData 数据查询逻辑
     */
    private int update(String sql, Map<String, Object> paramMap, UpdateData updateData) {
        Assert.hasText(sql, "sql不能为空");
        sql = StringUtils.trim(sql);
        int res = updateData.execute(new JdbcContext(sql, paramMap));
        SqlLoggerUtils.printfUpdateTotal(res);
        return res;
    }

    /**
     * 批量执行更新SQL，返回更新影响数据量
     *
     * @param sql    sql脚本，参数格式[:param]
     * @param params 参数集合，参数格式[:param]
     */
    private int[] batchUpdate(String sql, List<?> params, BatchUpdateData batchUpdateData) {
        Assert.hasText(sql, "sql不能为空");
        Assert.notNull(params, "参数数组不能为空");
        sql = StringUtils.trim(sql);
        final List<Map<String, Object>> paramList = new ArrayList<>(params.size());
        for (Object param : params) {
            Map<String, Object> map = BeanCopyUtils.toMap(param);
            paramList.add(map);
        }
        int[] res = batchUpdateData.execute(new JdbcContext(sql, paramList));
        SqlLoggerUtils.printfUpdateTotal(res);
        return res;
    }

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sql        sql脚本，参数格式[:param]
     * @param paramMap   参数，参数格式[:param]
     * @param insertData 数据查询逻辑
     */
    private InsertResult insert(String sql, Map<String, Object> paramMap, InsertData insertData) {
        Assert.hasText(sql, "sql不能为空");
        sql = StringUtils.trim(sql);
        InsertResult insertResult = insertData.execute(new JdbcContext(sql, paramMap));
        SqlLoggerUtils.printfUpdateTotal(insertResult.getInsertCount());
        return insertResult;
    }

    /**
     * 创建事务执行模板对象
     *
     * @param isolationLevel      设置事务隔离级别 {@link TransactionDefinition#ISOLATION_DEFAULT}
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @param readOnly            设置事务是否只读
     * @param timeout             设置事务超时时间(单位：秒)
     * @see TransactionDefinition
     */
    private TransactionTemplate createTransactionDefinition(int isolationLevel, int propagationBehavior, boolean readOnly, int timeout) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setName(getNextTransactionName());
        transactionDefinition.setPropagationBehavior(propagationBehavior);
        transactionDefinition.setTimeout(timeout);
        transactionDefinition.setIsolationLevel(isolationLevel);
        transactionDefinition.setReadOnly(readOnly);
        return new TransactionTemplate(transactionManager, transactionDefinition);
    }

    /**
     * 获取下一个事务名称
     */
    public String getNextTransactionName() {
        int nextSerialNumber = transactionSerialNumber.incrementAndGet();
        String dsName = StringUtils.trimToEmpty(dataSourceName);
        String transactionName;
        if (nextSerialNumber < 0) {
            transactionName = TRANSACTION_NAME_PREFIX + nextSerialNumber + ":" + dsName;
        } else {
            transactionName = TRANSACTION_NAME_PREFIX + "+" + nextSerialNumber + ":" + dsName;
        }
        return transactionName;
    }

    /**
     * 获取存储过程调用对象
     *
     * @param procedureName 存储过程名称
     */
    private SimpleJdbcCall getJdbcCall(String procedureName) {
        return PROCEDURE_CACHE.computeIfAbsent(
                String.format("%s@%s", dataSourceName, procedureName),
                key -> {
                    SimpleJdbcCall procedure = new ProcedureJdbcCall(this)
                            .withProcedureName(procedureName)
                            .withNamedBinding();
                    procedure.compile();
                    return procedure;
                }
        );
    }

    /**
     * 执行更新操作(需要调用方控制事务和资源的释放)
     *
     * @param con    数据库连接对象
     * @param sql    sql脚本，参数格式[?]
     * @param params sql参数
     * @return 影响的行数
     */
    private static int executeUpdate(Connection con, String sql, Object[] params) throws SQLException {
        PreparedStatement preparedStatement = null;
        // 执行SQL预编译
        preparedStatement = con.prepareStatement(sql);
        // 绑定参数设置sql占位符中的值
        if (params != null) {
            ArgumentPreparedStatementSetter pss = new ArgumentPreparedStatementSetter(params);
            pss.setValues(preparedStatement);
        }
        // 执行sql
        SqlLoggerUtils.printfSql(sql,  params);
        return preparedStatement.executeUpdate();
    }

    /**
     * 执行查询操作(需要调用方控制事务和资源的释放)
     *
     * @param con    数据库连接对象
     * @param sql    sql脚本，参数格式[?]
     * @param params sql参数
     */
    private static List<Map<String, Object>> executeQuery(Connection con, String sql, Object[] params) throws SQLException {
        // 执行SQL预编译
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        // 设置sql占位符中的值
        if (params != null) {
            ArgumentPreparedStatementSetter pss = new ArgumentPreparedStatementSetter(params);
            pss.setValues(preparedStatement);
        }
        // 执行sql
        SqlLoggerUtils.printfSql(sql,  params);
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            MapRowMapper mapRowMapper = new MapRowMapper(RenameStrategy.None);
            RowMapperResultSetExtractor<Map<String, Object>> resultExtractor = new RowMapperResultSetExtractor<>(mapRowMapper);
            return resultExtractor.extractData(resultSet);
        }
    }

    // --------------------------------------------------------------------------------------------
    //  SQL执行器
    // --------------------------------------------------------------------------------------------

    @Data
    protected static class JdbcContext {
        /**
         * SQL语句
         */
        private final String sql;
        /**
         * SQL参数
         */
        private final Map<String, Object> paramMap;
        /**
         * 批量SQL参数
         */
        private final List<Map<String, Object>> paramList;

        public JdbcContext(String sql, Map<String, Object> paramMap) {
            this.sql = sql;
            this.paramMap = Optional.ofNullable(paramMap).orElse(new HashMap<>());
            this.paramList = null;
        }

        public JdbcContext(String sql, List<Map<String, Object>> paramList) {
            this.sql = sql;
            this.paramMap = null;
            this.paramList = Optional.ofNullable(paramList).orElse(new ArrayList<>());
        }
    }

    @FunctionalInterface
    protected interface JdbcExecute<T> {
        T execute(JdbcContext context);
    }

    @Data
    protected static class QueryMetaData<T> implements JdbcExecute<T> {
        private final Jdbc jdbc;
        private final ResultSetExtractor<T> resultSetExtractor;

        @Override
        public T execute(JdbcContext context) {
            Page<?> page = new Page<>(1, 1);
            Map<String, Object> paramMap = new HashMap<>(context.getParamMap());
            String pageSql = DialectFactory.buildPaginationSql(page, context.getSql(), paramMap, jdbc.getDbType(), null);
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(pageSql, paramMap);
                return jdbc.jdbcTemplate.query(pageSql, paramMap, resultSetExtractor);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class QueryObject<T> implements JdbcExecute<T> {
        private final Jdbc jdbc;
        private final Class<T> returnType;
        private final boolean queryFirst;

        public QueryObject(Jdbc jdbc, Class<T> returnType, boolean queryFirst) {
            this.jdbc = jdbc;
            this.returnType = returnType;
            this.queryFirst = queryFirst;
        }

        public QueryObject(Jdbc jdbc, Class<T> returnType) {
            this(jdbc, returnType, false);
        }

        @Override
        public T execute(JdbcContext context) {
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                String sql = context.getSql();
                if (queryFirst) {
                    // 改写查询sql，限制查询数据量
                    sql = DialectFactory.buildPaginationSql(0, 1, sql, jdbc.dbType, null);
                }
                SqlLoggerUtils.printfSql(sql, context.getParamMap());
                List<T> list = jdbc.jdbcTemplate.query(sql, context.getParamMap(), new SingleColumnRowMapper<>(returnType));
                return DataAccessUtils.singleResult(list);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class QueryOne<T> implements JdbcExecute<T> {
        private final Jdbc jdbc;
        private final RowMapper<T> rowMapper;
        private final boolean queryFirst;

        public QueryOne(Jdbc jdbc, RowMapper<T> rowMapper, boolean queryFirst) {
            this.jdbc = jdbc;
            this.rowMapper = rowMapper;
            this.queryFirst = queryFirst;
        }

        public QueryOne(Jdbc jdbc, RowMapper<T> rowMapper) {
            this(jdbc, rowMapper, false);
        }

        @Override
        public T execute(JdbcContext context) {
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                String sql = context.getSql();
                if (queryFirst) {
                    // 改写查询sql，限制查询数据量
                    sql = DialectFactory.buildPaginationSql(0, 1, sql, jdbc.dbType, null);
                }
                SqlLoggerUtils.printfSql(sql, context.getParamMap());
                List<T> list = jdbc.jdbcTemplate.query(sql, context.getParamMap(), rowMapper);
                return DataAccessUtils.singleResult(list);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class QueryMany<T> implements JdbcExecute<List<T>> {
        private final Jdbc jdbc;
        private final RowMapper<T> rowMapper;

        @Override
        public List<T> execute(JdbcContext context) {
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(context.getSql(), context.getParamMap());
                return jdbc.jdbcTemplate.query(context.getSql(), new MapSqlParameterSource(context.getParamMap()), rowMapper);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class QueryForCursor implements JdbcExecute<Void> {
        private final Jdbc jdbc;
        private final InterruptRowCallbackHandler interruptRowCallbackHandler;

        @Override
        public Void execute(JdbcContext context) {
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(context.getSql(), context.getParamMap());
                jdbc.jdbcTemplate.query(context.getSql(), new MapSqlParameterSource(context.getParamMap()), (ResultSetExtractor<?>) interruptRowCallbackHandler);
                return null;
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class UpdateData implements JdbcExecute<Integer> {
        private final Jdbc jdbc;

        @Override
        public Integer execute(JdbcContext context) {
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(context.getSql(), context.getParamMap());
                return jdbc.jdbcTemplate.update(context.getSql(), new MapSqlParameterSource(context.getParamMap()));
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    protected static class BatchUpdateData implements JdbcExecute<int[]> {
        private final Jdbc jdbc;

        @Override
        public int[] execute(JdbcContext context) {
            final List<Map<String, Object>> paramList = context.getParamList();
            final SqlParameterSource[] paramArray = new SqlParameterSource[paramList.size()];
            int index = 0;
            for (Map<String, Object> map : paramList) {
                paramArray[index] = new MapSqlParameterSource(map);
                index++;
            }
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(context.getSql(), paramList);
                return jdbc.jdbcTemplate.batchUpdate(context.getSql(), paramArray);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }

    @Data
    private static class InsertData implements JdbcExecute<InsertResult> {
        private final Jdbc jdbc;

        @Override
        public InsertResult execute(JdbcContext context) {
            final Map<String, Object> paramMap = context.getParamMap();
            SqlParameterSource sqlParameterSource;
            if (paramMap != null && paramMap.size() > 0) {
                sqlParameterSource = new MapSqlParameterSource(paramMap);
            } else {
                sqlParameterSource = new EmptySqlParameterSource();
            }
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.listeners.beforeExec(jdbc.dbType, jdbc.jdbcTemplate);
            Exception exception = null;
            try {
                SqlLoggerUtils.printfSql(context.getSql(), paramMap);
                int insertCount = jdbc.jdbcTemplate.update(context.getSql(), sqlParameterSource, keyHolder);
                List<Map<String, Object>> keysList = keyHolder.getKeyList();
                InsertResult.KeyHolder resultKeyHolder = new InsertResult.KeyHolder(keysList);
                return new InsertResult(insertCount, resultKeyHolder);
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                jdbc.listeners.afterExec(jdbc.dbType, jdbc.jdbcTemplate, exception);
            }
        }
    }
}
