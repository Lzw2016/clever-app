package org.clever.data.jdbc;

import lombok.Getter;
import org.clever.core.RenameStrategy;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.IPage;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.support.*;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/02 19:53 <br/>
 */
public class MyBatis extends AbstractDataSource {
    /**
     * JDBC数据源
     */
    @Getter
    private final Jdbc jdbc;
    /**
     * SQL ID基础路径
     */
    @Getter
    private final String stdXmlPath;
    /**
     * 项目列表(优选级又高到底)
     */
    @Getter
    private final List<String> projects;
    /**
     * Mapper动态SQL
     */
    @Getter
    private final MyBatisMapperSql mapperSql;

    public MyBatis(Jdbc jdbc, String stdXmlPath, List<String> projects, MyBatisMapperSql mapperSql) {
        Assert.notNull(jdbc, "参数jdbc不能为空");
        Assert.hasText(stdXmlPath, "参数stdXmlPath不能为空");
        Assert.notNull(mapperSql, "参数mapperSql不能为空");
        this.jdbc = jdbc;
        this.stdXmlPath = stdXmlPath;
        if (projects == null) {
            projects = Collections.emptyList();
        }
        this.projects = Collections.unmodifiableList(projects);
        this.mapperSql = mapperSql;
    }

    /**
     * 获取数据库类型
     */
    public DbType getDbType() {
        return jdbc.getDbType();
    }

    @Override
    public boolean isClosed() {
        return jdbc.isClosed();
    }

    @Override
    public void close() {
        jdbc.close();
    }

    // --------------------------------------------------------------------------------------------
    // Query 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<DbColumnMetaData> queryMetaData(String sqlId, Object param, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryMetaData(sqlInfo.getValue1(), sqlInfo.getValue2(), resultRename);
    }

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public List<DbColumnMetaData> queryMetaData(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryMetaData(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 获取sql查询返回的表头元数据
     *
     * @param sqlId SQL ID
     */
    public List<DbColumnMetaData> queryMetaData(String sqlId) {
        return jdbc.queryMetaData(getSql(sqlId));
    }

    /**
     * 查询一条数据，返回一个Map
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sqlId, Object param, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryOne(sqlInfo.getValue1(), sqlInfo.getValue2(), resultRename);
    }

    /**
     * 查询一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Map<String, Object> queryOne(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryOne(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询一条数据，返回一个Map
     *
     * @param sqlId        SQL ID
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sqlId, RenameStrategy resultRename) {
        return jdbc.queryOne(getSql(sqlId), resultRename);
    }

    /**
     * 查询一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     */
    public Map<String, Object> queryOne(String sqlId) {
        return jdbc.queryOne(getSql(sqlId));
    }

    /**
     * 查询一条数据，返回一个实体对象
     *
     * @param sqlId SQL ID
     * @param param 参数
     * @param clazz 查询对象类型
     */
    public <T> T queryOne(String sqlId, Object param, Class<T> clazz) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryOne(sqlInfo.getValue1(), sqlInfo.getValue2(), clazz);
    }

    /**
     * 查询一条数据，返回一个实体对象
     *
     * @param sqlId SQL ID
     * @param clazz 查询对象类型
     */
    public <T> T queryOne(String sqlId, Class<T> clazz) {
        return jdbc.queryOne(getSql(sqlId), clazz);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryMany(String sqlId, Object param, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryMany(sqlInfo.getValue1(), sqlInfo.getValue2(), resultRename);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public List<Map<String, Object>> queryMany(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryMany(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sqlId        SQL ID
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryMany(String sqlId, RenameStrategy resultRename) {
        return jdbc.queryMany(getSql(sqlId), resultRename);
    }

    /**
     * 查询多条数据，返回一个Map集合
     *
     * @param sqlId SQL ID
     */
    public List<Map<String, Object>> queryMany(String sqlId) {
        return jdbc.queryMany(getSql(sqlId));
    }

    /**
     * 查询多条数据，返回一个实体集合
     *
     * @param sqlId SQL ID
     * @param param 参数
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryMany(String sqlId, Object param, Class<T> clazz) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryMany(sqlInfo.getValue1(), sqlInfo.getValue2(), clazz);
    }

    /**
     * 查询多条数据，返回一个实体集合
     *
     * @param sqlId SQL ID
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryMany(String sqlId, Class<T> clazz) {
        return jdbc.queryMany(getSql(sqlId), clazz);
    }

    /**
     * 查询返回一个 String
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public String queryString(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryString(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 String
     *
     * @param sqlId SQL ID
     */
    public String queryString(String sqlId) {
        return jdbc.queryString(getSql(sqlId));
    }

    /**
     * 查询返回一个 Long
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Long queryLong(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryLong(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Long
     *
     * @param sqlId SQL ID
     */
    public Long queryLong(String sqlId) {
        return jdbc.queryLong(getSql(sqlId));
    }

    /**
     * 查询返回一个 Double
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Double queryDouble(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryDouble(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Double
     *
     * @param sqlId SQL ID
     */
    public Double queryDouble(String sqlId) {
        return jdbc.queryDouble(getSql(sqlId));
    }

    /**
     * 查询返回一个 BigDecimal
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public BigDecimal queryBigDecimal(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryBigDecimal(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 BigDecimal
     *
     * @param sqlId SQL ID
     */
    public BigDecimal queryBigDecimal(String sqlId) {
        return jdbc.queryBigDecimal(getSql(sqlId));
    }

    /**
     * 查询返回一个 Boolean
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Boolean queryBoolean(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryBoolean(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Boolean
     *
     * @param sqlId SQL ID
     */
    public Boolean queryBoolean(String sqlId) {
        return jdbc.queryBoolean(getSql(sqlId));
    }

    /**
     * 查询返回一个 Date
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Date queryDate(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryDate(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Date
     *
     * @param sqlId SQL ID
     */
    public Date queryDate(String sqlId) {
        return jdbc.queryDate(getSql(sqlId));
    }

    /**
     * 查询返回一个 Timestamp
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Timestamp queryTimestamp(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryTimestamp(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Timestamp
     *
     * @param sqlId SQL ID
     */
    public Timestamp queryTimestamp(String sqlId) {
        return jdbc.queryTimestamp(getSql(sqlId), Collections.emptyMap());
    }

    /**
     * SQL Count(获取一个SQL返回的数据总量)
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public long queryCount(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryCount(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * SQL Count(获取一个SQL返回的数据总量)
     *
     * @param sqlId SQL ID
     */
    public long queryCount(String sqlId) {
        return jdbc.queryCount(getSql(sqlId));
    }

    /**
     * 排序查询
     *
     * @param sqlId        SQL ID
     * @param sort         排序配置
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryBySort(String sqlId, QueryBySort sort, Object param, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryBySort(sqlInfo.getValue1(), sort, sqlInfo.getValue2(), resultRename);
    }

    /**
     * 排序查询
     *
     * @param sqlId SQL ID
     * @param sort  排序配置
     * @param param 参数
     */
    public List<Map<String, Object>> queryBySort(String sqlId, QueryBySort sort, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryBySort(sqlInfo.getValue1(), sort, sqlInfo.getValue2());
    }

    /**
     * 排序查询
     *
     * @param sqlId        SQL ID
     * @param sort         排序配置
     * @param resultRename 返回数据字段名重命名策略
     */
    public List<Map<String, Object>> queryBySort(String sqlId, QueryBySort sort, RenameStrategy resultRename) {
        return jdbc.queryBySort(getSql(sqlId), sort, resultRename);
    }

    /**
     * 排序查询
     *
     * @param sqlId SQL ID
     * @param sort  排序配置
     */
    public List<Map<String, Object>> queryBySort(String sqlId, QueryBySort sort) {
        return jdbc.queryBySort(getSql(sqlId), sort);
    }

    /**
     * 排序查询
     *
     * @param sqlId SQL ID
     * @param sort  排序配置
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryBySort(String sqlId, QueryBySort sort, Class<T> clazz) {
        return jdbc.queryBySort(getSql(sqlId), sort, clazz);
    }

    /**
     * 排序查询
     *
     * @param sqlId SQL ID
     * @param sort  排序配置
     * @param param 参数
     * @param clazz 查询对象类型
     */
    public <T> List<T> queryBySort(String sqlId, QueryBySort sort, Object param, Class<T> clazz) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryBySort(sqlInfo.getValue1(), sort, sqlInfo.getValue2(), clazz);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId        SQL ID
     * @param pagination   分页配置(支持排序)
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public IPage<Map<String, Object>> queryByPage(String sqlId, QueryByPage pagination, Object param, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryByPage(sqlInfo.getValue1(), pagination, sqlInfo.getValue2(), resultRename);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId      SQL ID
     * @param pagination 分页配置(支持排序)
     * @param param      参数
     */
    public IPage<Map<String, Object>> queryByPage(String sqlId, QueryByPage pagination, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryByPage(sqlInfo.getValue1(), pagination, sqlInfo.getValue2());
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId        SQL ID
     * @param pagination   分页配置(支持排序)
     * @param resultRename 返回数据字段名重命名策略
     */
    public IPage<Map<String, Object>> queryByPage(String sqlId, QueryByPage pagination, RenameStrategy resultRename) {
        return jdbc.queryByPage(getSql(sqlId), pagination, resultRename);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId      SQL ID
     * @param pagination 分页配置(支持排序)
     */
    public IPage<Map<String, Object>> queryByPage(String sqlId, QueryByPage pagination) {
        return jdbc.queryByPage(getSql(sqlId), pagination);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId      SQL ID
     * @param pagination 分页配置(支持排序)
     * @param param      参数
     * @param clazz      查询对象类型
     */
    public <T> IPage<T> queryByPage(String sqlId, QueryByPage pagination, Object param, Class<T> clazz) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryByPage(sqlInfo.getValue1(), pagination, sqlInfo.getValue2(), clazz);
    }

    /**
     * 分页查询(支持排序)，返回分页对象
     *
     * @param sqlId      SQL ID
     * @param pagination 分页配置(支持排序)
     * @param clazz      查询对象类型
     */
    public <T> IPage<T> queryByPage(String sqlId, QueryByPage pagination, Class<T> clazz) {
        return jdbc.queryByPage(getSql(sqlId), pagination, clazz);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param batchSize    一个批次的数据量
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Object param, int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), batchSize, consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId     SQL ID
     * @param param     参数
     * @param batchSize 一个批次的数据量
     * @param consumer  游标批次读取数据消费者
     */
    public void queryForCursor(String sqlId, Object param, int batchSize, Consumer<BatchData> consumer) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), batchSize, consumer);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param batchSize    一个批次的数据量
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, int batchSize, Consumer<BatchData> consumer, RenameStrategy resultRename) {
        jdbc.queryForCursor(getSql(sqlId), batchSize, consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId     SQL ID
     * @param batchSize 一个批次的数据量
     * @param consumer  游标批次读取数据消费者
     */
    public void queryForCursor(String sqlId, int batchSize, Consumer<BatchData> consumer) {
        jdbc.queryForCursor(getSql(sqlId), batchSize, consumer);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Object param, Consumer<RowData> consumer, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId    SQL ID
     * @param param    参数
     * @param consumer 游标批次读取数据消费者
     */
    public void queryForCursor(String sqlId, Object param, Consumer<RowData> consumer) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), consumer);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param consumer     游标批次读取数据消费者
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Consumer<RowData> consumer, RenameStrategy resultRename) {
        jdbc.queryForCursor(getSql(sqlId), consumer, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId    SQL ID
     * @param consumer 游标批次读取数据消费者
     */
    public void queryForCursor(String sqlId, Consumer<RowData> consumer) {
        jdbc.queryForCursor(getSql(sqlId), consumer);
    }

    // --------------------------------------------------------------------------------------------
    // Update 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sqlId    SqlID
     * @param paramMap 查询参数
     */
    public int update(String sqlId, Object paramMap) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, paramMap);
        return jdbc.update(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 执行更新SQL，返回更新影响数据量
     *
     * @param sqlId SqlID
     */
    public int update(String sqlId) {
        return jdbc.update(getSql(sqlId));
    }

    /**
     * 批量执行更新SQL，返回更新影响数据量
     *
     * @param sqlId  SqlID
     * @param params 参数集合
     */
    public int[] batchUpdate(String sqlId, List<?> params) {
        return jdbc.batchUpdate(getSql(sqlId), params);
    }

    // --------------------------------------------------------------------------------------------
    // Insert 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sqlId    SqlID
     * @param paramMap 查询参数
     */
    public InsertResult insert(String sqlId, Object paramMap) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, paramMap);
        return jdbc.insert(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 执行insert SQL，返回数据库自增主键值和新增数据量
     *
     * @param sqlId SqlID
     */
    public InsertResult insert(String sqlId) {
        return jdbc.insert(getSql(sqlId));
    }

    // --------------------------------------------------------------------------------------------
    //  事务操作
    // --------------------------------------------------------------------------------------------

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly            设置事务是否只读
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel, boolean readOnly) {
        return jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel, readOnly);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return jdbc.beginTX(action, propagationBehavior, timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior) {
        return jdbc.beginTX(action, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action) {
        return jdbc.beginTX(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior, timeout, isolationLevel);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior, timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return jdbc.beginReadOnlyTX(action);
    }

    // --------------------------------------------------------------------------------------------
    //  其它 操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取数据源信息
     */
    public JdbcInfo getInfo() {
        return jdbc.getInfo();
    }

    /**
     * 获取数据源状态
     */
    public JdbcDataSourceStatus getStatus() {
        return jdbc.getStatus();
    }

    // --------------------------------------------------------------------------------------------
    //  内部函数
    // --------------------------------------------------------------------------------------------

    private TupleTwo<String, Map<String, Object>> getSql(String sqlId, Object parameter) {
        Assert.hasText(sqlId, "参数sqlId不能为空");
        if (parameter == null) {
            parameter = new HashMap<>();
        }
        BoundSql boundSql = getBoundSql(sqlId, parameter);
        Assert.notNull(boundSql, "SQL不存在，sqlId=" + sqlId);
        return TupleTwo.creat(boundSql.getNamedParameterSql(), boundSql.getParameterMap());
    }

    private String getSql(String sqlId) {
        Assert.hasText(sqlId, "参数sqlId不能为空");
        BoundSql boundSql = getBoundSql(sqlId, new HashMap<>());
        return boundSql.getNamedParameterSql();
    }

    private BoundSql getBoundSql(String sqlId, Object parameter) {
        SqlSource sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, jdbc.getDbType(), projects.toArray(new String[0]));
        return sqlSource.getBoundSql(jdbc.getDbType(), parameter);
    }
}