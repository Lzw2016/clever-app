package org.clever.data.jdbc;

import lombok.Getter;
import lombok.SneakyThrows;
import org.clever.core.RenameStrategy;
import org.clever.core.function.ZeroConsumer;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.IPage;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.AbstractDataSource;
import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.support.*;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionStatus;
import org.clever.transaction.annotation.Isolation;
import org.clever.transaction.annotation.Propagation;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
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
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Map<String, Object> queryOne(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryOne(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sqlId        SQL ID
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryOne(String sqlId, RenameStrategy resultRename) {
        return jdbc.queryOne(getSql(sqlId), resultRename);
    }

    /**
     * 查询一条数据，返回一个Map(sql返回多条数据会抛出异常)
     *
     * @param sqlId SQL ID
     */
    public Map<String, Object> queryOne(String sqlId) {
        return jdbc.queryOne(getSql(sqlId));
    }

    /**
     * 查询一条数据，返回一个实体对象(sql返回多条数据会抛出异常)
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
     * 查询一条数据，返回一个实体对象(sql返回多条数据会抛出异常)
     *
     * @param sqlId SQL ID
     * @param clazz 查询对象类型
     */
    public <T> T queryOne(String sqlId, Class<T> clazz) {
        return jdbc.queryOne(getSql(sqlId), clazz);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId        SQL ID
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryFirst(String sqlId, RenameStrategy resultRename) {
        return jdbc.queryFirst(getSql(sqlId), resultRename);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     */
    public Map<String, Object> queryFirst(String sqlId) {
        return jdbc.queryFirst(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId        SQL ID
     * @param param        参数
     * @param resultRename 返回数据字段名重命名策略
     */
    public Map<String, Object> queryFirst(String sqlId, Object param, RenameStrategy resultRename) {
        return jdbc.queryFirst(getSql(sqlId), param, resultRename);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Map<String, Object> queryFirst(String sqlId, Object param) {
        return jdbc.queryFirst(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     * @param clazz 查询对象类型
     */
    public <T> T queryFirst(String sqlId, Class<T> clazz) {
        return jdbc.queryFirst(getSql(sqlId), clazz);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个Map
     *
     * @param sqlId SQL ID
     * @param param 参数
     * @param clazz 查询对象类型
     */
    public <T> T queryFirst(String sqlId, Object param, Class<T> clazz) {
        return jdbc.queryFirst(getSql(sqlId), param, clazz);
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
     * 查询返回一个 Integer
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Integer queryInt(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryInt(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Integer
     *
     * @param sqlId SQL ID
     */
    public Integer queryInt(String sqlId) {
        return jdbc.queryInt(getSql(sqlId));
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
     * 查询返回一个 Float
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Float queryFloat(String sqlId, Object param) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        return jdbc.queryFloat(sqlInfo.getValue1(), sqlInfo.getValue2());
    }

    /**
     * 查询返回一个 Float
     *
     * @param sqlId SQL ID
     */
    public Float queryFloat(String sqlId) {
        return jdbc.queryFloat(getSql(sqlId));
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
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public String queryFirstString(String sqlId, Object param) {
        return jdbc.queryFirstString(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 String
     *
     * @param sqlId SQL ID
     */
    public String queryFirstString(String sqlId) {
        return jdbc.queryFirstString(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Long
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Long queryFirstLong(String sqlId, Object param) {
        return jdbc.queryFirstLong(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Long
     *
     * @param sqlId SQL ID
     */
    public Long queryFirstLong(String sqlId) {
        return jdbc.queryFirstLong(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Double
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Double queryFirstDouble(String sqlId, Object param) {
        return jdbc.queryFirstDouble(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Double
     *
     * @param sqlId SQL ID
     */
    public Double queryFirstDouble(String sqlId) {
        return jdbc.queryFirstDouble(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 BigDecimal
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public BigDecimal queryFirstBigDecimal(String sqlId, Object param) {
        return jdbc.queryFirstBigDecimal(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 BigDecimal
     *
     * @param sqlId SQL ID
     */
    public BigDecimal queryFirstBigDecimal(String sqlId) {
        return jdbc.queryFirstBigDecimal(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Boolean
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Boolean queryFirstBoolean(String sqlId, Object param) {
        return jdbc.queryFirstBoolean(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Boolean
     *
     * @param sqlId SQL ID
     */
    public Boolean queryFirstBoolean(String sqlId) {
        return jdbc.queryFirstBoolean(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Date
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Date queryFirstDate(String sqlId, Object param) {
        return jdbc.queryFirstDate(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Date
     *
     * @param sqlId SQL ID
     */
    public Date queryFirstDate(String sqlId) {
        return jdbc.queryFirstDate(getSql(sqlId));
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Timestamp
     *
     * @param sqlId SQL ID
     * @param param 参数
     */
    public Timestamp queryFirstTimestamp(String sqlId, Object param) {
        return jdbc.queryFirstTimestamp(getSql(sqlId), param);
    }

    /**
     * 查询sql执行结果的第一条数据，返回一个 Timestamp
     *
     * @param sqlId SQL ID
     */
    public Timestamp queryFirstTimestamp(String sqlId) {
        return jdbc.queryFirstTimestamp(getSql(sqlId));
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
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Object param, int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), batchSize, callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId     SQL ID
     * @param param     参数
     * @param batchSize 一个批次的数据量
     * @param callback  游标批次读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sqlId, Object param, int batchSize, Function<BatchData, Boolean> callback) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), batchSize, callback);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param batchSize    一个批次的数据量
     * @param callback     游标批次读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, int batchSize, Function<BatchData, Boolean> callback, RenameStrategy resultRename) {
        jdbc.queryForCursor(getSql(sqlId), batchSize, callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId     SQL ID
     * @param batchSize 一个批次的数据量
     * @param callback  游标批次读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sqlId, int batchSize, Function<BatchData, Boolean> callback) {
        jdbc.queryForCursor(getSql(sqlId), batchSize, callback);
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
     * @param callback     游标读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Object param, Function<RowData, Boolean> callback, RenameStrategy resultRename) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId    SQL ID
     * @param param    参数
     * @param callback 游标读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sqlId, Object param, Function<RowData, Boolean> callback) {
        TupleTwo<String, Map<String, Object>> sqlInfo = getSql(sqlId, param);
        jdbc.queryForCursor(sqlInfo.getValue1(), sqlInfo.getValue2(), callback);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId        SQL ID
     * @param callback     游标读取数据回调(返回true则中断数据读取)
     * @param resultRename 返回数据字段名重命名策略
     */
    public void queryForCursor(String sqlId, Function<RowData, Boolean> callback, RenameStrategy resultRename) {
        jdbc.queryForCursor(getSql(sqlId), callback, resultRename);
    }

    /**
     * 查询多条数据(大量数据)，使用游标读取
     *
     * @param sqlId    SQL ID
     * @param callback 游标读取数据回调(返回true则中断数据读取)
     */
    public void queryForCursor(String sqlId, Function<RowData, Boolean> callback) {
        jdbc.queryForCursor(getSql(sqlId), callback);
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
    //  调用存储过程
    // --------------------------------------------------------------------------------------------

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param paramMap      参数
     */
    public Map<String, Object> callGet(final String procedureName, Map<String, ?> paramMap) {
        return jdbc.callGet(procedureName, paramMap);
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public Map<String, Object> callGet(final String procedureName, Object... params) {
        return jdbc.callGet(procedureName, params);
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public Map<String, Object> callGet(String procedureName, List<?> params) {
        return jdbc.callGet(procedureName, params);
    }

    /**
     * 执行存储过程(以Map形式返回数据)
     *
     * @param procedureName 存贮过程名称
     */
    public Map<String, Object> callGet(String procedureName) {
        return jdbc.callGet(procedureName);
    }

    /**
     * 执行存储过程
     *
     * @param procedureName 存贮过程名称
     * @param params        参数
     */
    public void call(String procedureName, Object... params) {
        jdbc.call(procedureName, params);
    }

    /**
     * 执行存储过程
     *
     * @param procedureName 存贮过程名称
     */
    public void call(String procedureName) {
        jdbc.call(procedureName);
    }

    // --------------------------------------------------------------------------------------------
    //  事务操作
    // --------------------------------------------------------------------------------------------

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.clever.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly            设置事务是否只读
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel, boolean readOnly) {
        return jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel, readOnly);
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
        return jdbc.beginTX(action, propagation.value(), timeout, isolation.value(), readOnly);
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
        jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel, readOnly);
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
        jdbc.beginTX(action, propagation.value(), timeout, isolation.value(), readOnly);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.clever.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel);
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
        return jdbc.beginTX(action, propagation.value(), timeout, isolation.value());
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
        jdbc.beginTX(action, propagationBehavior, timeout, isolationLevel);
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
        jdbc.beginTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return jdbc.beginTX(action, propagationBehavior, timeout);
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
        return jdbc.beginTX(action, propagation.value(), timeout);
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
        jdbc.beginTX(action, propagationBehavior, timeout);
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
        jdbc.beginTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior) {
        return jdbc.beginTX(action, propagationBehavior);
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
        return jdbc.beginTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, int propagationBehavior) {
        jdbc.beginTX(action, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action, Propagation propagation) {
        jdbc.beginTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action) {
        return jdbc.beginTX(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @see TransactionDefinition
     */
    public void beginTX(Consumer<TransactionStatus> action) {
        jdbc.beginTX(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.clever.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior, timeout, isolationLevel);
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
        return jdbc.beginReadOnlyTX(action, propagation.value(), timeout, isolation.value());
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
        jdbc.beginReadOnlyTX(action, propagationBehavior, timeout, isolationLevel);
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
        jdbc.beginReadOnlyTX(action, propagation.value(), timeout, isolation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior, timeout);
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
        return jdbc.beginReadOnlyTX(action, propagation.value(), timeout);
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
        jdbc.beginReadOnlyTX(action, propagationBehavior, timeout);
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
        jdbc.beginReadOnlyTX(action, propagation.value(), timeout);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.clever.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior) {
        return jdbc.beginReadOnlyTX(action, propagationBehavior);
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
        return jdbc.beginReadOnlyTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, int propagationBehavior) {
        jdbc.beginReadOnlyTX(action, propagationBehavior);
    }

    /**
     * 在事务内支持操作
     *
     * @param action      事务内数据库操作
     * @param propagation 设置事务传递性 {@link TransactionDefinition#PROPAGATION_REQUIRED}
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action, Propagation propagation) {
        jdbc.beginReadOnlyTX(action, propagation.value());
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.clever.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return jdbc.beginReadOnlyTX(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @see TransactionDefinition
     */
    public void beginReadOnlyTX(Consumer<TransactionStatus> action) {
        jdbc.beginReadOnlyTX(action);
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
        return jdbc.getSqlWarning(clear);
    }

    /**
     * 获取SQLWarning输出(支持Oracle的dbms_output输出)
     */
    public String getSqlWarning() {
        return jdbc.getSqlWarning();
    }

    /**
     * 启用收集SQLWarning输出(支持Oracle的dbms_output输出)
     */
    public void enableSqlWarning() {
        jdbc.enableSqlWarning();
    }

    /**
     * 禁用收集SQLWarning输出(支持Oracle的dbms_output输出)
     *
     * @return 返回之前输出的数据 & 清空数据
     */
    public String disableSqlWarning() {
        return jdbc.disableSqlWarning();
    }

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
    //  业务含义操作
    // --------------------------------------------------------------------------------------------

    /**
     * 获取数据库服务器当前时间
     */
    public Date currentDate() {
        return jdbc.currentDate();
    }

    /**
     * 返回下一个序列值
     *
     * @param seqName 序列名称
     */
    public Long nextSeq(String seqName) {
        return jdbc.nextSeq(seqName);
    }

    /***
     * 批量获取唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     * @param size 唯一id值数量(1 ~ 10W)
     */
    public List<Long> nextIds(String idName, int size) {
        return jdbc.nextIds(idName, size);
    }

    /**
     * 返回下一个唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     */
    public Long nextId(String idName) {
        return jdbc.nextId(idName);
    }

    /**
     * 返回当前唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param idName 唯一id名称
     */
    public Long currentId(String idName) {
        return jdbc.currentId(idName);
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
        return jdbc.nextCodes(codeName, size);
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
        return jdbc.nextCode(codeName);
    }

    /**
     * 批量获取唯一的 code 值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param codeName code名称
     */
    public String currentCode(String codeName) {
        return jdbc.currentCode(codeName);
    }

    /**
     * 借助数据库行级锁实现的分布式排他锁 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>{@code
     *   tryLock("lockName", waitSeconds, locked -> {
     *      if(locked) {
     *          // 同步业务逻辑处理...
     *      }
     *      return result;
     *   })
     * }</pre>
     *
     * @param lockName    锁名称
     * @param waitSeconds 等待锁的最大时间(小于等于0表示一直等待)
     * @param syncBlock   同步代码块(可保证分布式串行执行)
     */
    public <T> T tryLock(String lockName, int waitSeconds, Function<Boolean, T> syncBlock) {
        return jdbc.tryLock(lockName, waitSeconds, syncBlock);
    }

    /**
     * 借助数据库行级锁实现的分布式排他锁 <br/>
     * <b>此功能需要数据库表支持</b>
     * <pre>{@code
     *   tryLock("lockName", waitSeconds, locked -> {
     *      if(locked) {
     *          // 同步业务逻辑处理...
     *      }
     *   })
     * }</pre>
     *
     * @param lockName    锁名称
     * @param waitSeconds 等待锁的最大时间(小于等于0表示一直等待)
     * @param syncBlock   同步代码块(可保证分布式串行执行)
     */
    public void tryLock(String lockName, int waitSeconds, Consumer<Boolean> syncBlock) {
        jdbc.tryLock(lockName, waitSeconds, syncBlock);
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
        return jdbc.lock(lockName, syncBlock);
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
    public void lock(String lockName, ZeroConsumer syncBlock) {
        jdbc.lock(lockName, syncBlock);
    }

    /**
     * 直接使用数据库提供的lock功能实现的分布式排他锁 <br/>
     * <pre>{@code
     *   nativeTryLock("lockName", waitSeconds, locked -> {
     *      if(locked) {
     *          // 同步业务逻辑处理...
     *      }
     *      return result;
     *   })
     * }</pre>
     * <strong>
     * 注意: 如果调用上下文中没有开启事务，会自动开启一个新事务 syncBlock 会在这个事务环境中执行。
     * 如果调用上下文中已经存在事务，就不会开启事务，而是在当前的事务环境中执行数据库锁操作。
     * </strong>
     *
     * @param lockName    锁名称
     * @param waitSeconds 等待锁的最大时间
     * @param syncBlock   同步代码块(可保证分布式串行执行)
     */
    public <T> T nativeTryLock(String lockName, int waitSeconds, Function<Boolean, T> syncBlock) {
        return jdbc.nativeTryLock(lockName, waitSeconds, syncBlock);
    }

    /**
     * 直接使用数据库提供的lock功能实现的分布式排他锁 <br/>
     * <pre>{@code
     *   nativeTryLock("lockName", waitSeconds, locked -> {
     *      if(locked) {
     *          // 同步业务逻辑处理...
     *      }
     *      return result;
     *   })
     * }</pre>
     * <strong>
     * 注意: 如果调用上下文中没有开启事务，会自动开启一个新事务 syncBlock 会在这个事务环境中执行。
     * 如果调用上下文中已经存在事务，就不会开启事务，而是在当前的事务环境中执行数据库锁操作。
     * </strong>
     *
     * @param lockName    锁名称
     * @param waitSeconds 等待锁的最大时间
     * @param syncBlock   同步代码块(可保证分布式串行执行)
     */
    public void nativeTryLock(String lockName, int waitSeconds, Consumer<Boolean> syncBlock) {
        jdbc.nativeTryLock(lockName, waitSeconds, syncBlock);
    }

    /**
     * 直接使用数据库提供的lock功能实现的分布式排他锁 <br/>
     * <pre>{@code
     *   lock("lockName", () -> {
     *      // 同步业务逻辑处理...
     *   })
     * }</pre>
     * <strong>
     * 注意: 如果调用上下文中没有开启事务，会自动开启一个新事务 syncBlock 会在这个事务环境中执行。
     * 如果调用上下文中已经存在事务，就不会开启事务，而是在当前的事务环境中执行数据库锁操作。
     * </strong>
     *
     * @param lockName  锁名称
     * @param syncBlock 同步代码块(可保证分布式串行执行)
     */
    public <T> T nativeLock(String lockName, Supplier<T> syncBlock) {
        return jdbc.nativeLock(lockName, syncBlock);
    }

    /**
     * 直接使用数据库提供的lock功能实现的分布式排他锁 <br/>
     * <pre>{@code
     *   lock("lockName", () -> {
     *      // 同步业务逻辑处理...
     *   })
     * }</pre>
     * <strong>
     * 注意: 如果调用上下文中没有开启事务，会自动开启一个新事务 syncBlock 会在这个事务环境中执行。
     * 如果调用上下文中已经存在事务，就不会开启事务，而是在当前的事务环境中执行数据库锁操作。
     * </strong>
     *
     * @param lockName  锁名称
     * @param syncBlock 同步代码块(可保证分布式串行执行)
     */
    public void nativeLock(String lockName, ZeroConsumer syncBlock) {
        jdbc.nativeLock(lockName, syncBlock);
    }

    // --------------------------------------------------------------------------------------------
    //  获取 mybatis sql
    // --------------------------------------------------------------------------------------------

    /**
     * 根据SQL ID获取sql和参数
     *
     * @param sqlId     SQL ID
     * @param parameter 参数
     * @return {@code TupleTwo<sql, parameterMap>}
     */
    public TupleTwo<String, Map<String, Object>> getSql(String sqlId, Object parameter) {
        Assert.hasText(sqlId, "参数sqlId不能为空");
        if (parameter == null) {
            parameter = new HashMap<>();
        }
        BoundSql boundSql = getBoundSql(sqlId, parameter);
        Assert.notNull(boundSql, "SQL不存在，sqlId=" + sqlId);
        return TupleTwo.creat(boundSql.getNamedParameterSql(), boundSql.getParameterMap());
    }

    /**
     * 根据SQL ID获取sql
     *
     * @param sqlId SQL ID
     */
    public String getSql(String sqlId) {
        Assert.hasText(sqlId, "参数sqlId不能为空");
        BoundSql boundSql = getBoundSql(sqlId, new HashMap<>());
        return boundSql.getNamedParameterSql();
    }

    private BoundSql getBoundSql(String sqlId, Object parameter) {
        SqlSource sqlSource = mapperSql.getSqlSource(sqlId, stdXmlPath, jdbc.getDbType(), projects.toArray(new String[0]));
        return sqlSource.getBoundSql(jdbc.getDbType(), parameter);
    }
}
