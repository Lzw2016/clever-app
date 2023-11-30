package org.clever.data.jdbc;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.*;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import lombok.SneakyThrows;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.querydsl.SQLCoreListener;
import org.clever.data.jdbc.querydsl.sql.SQLQueryFactory;
import org.clever.data.jdbc.support.JdbcDataSourceStatus;
import org.clever.data.jdbc.support.JdbcInfo;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.TransactionStatus;
import org.clever.transaction.annotation.Isolation;
import org.clever.transaction.annotation.Propagation;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/22 09:52 <br/>
 */
@Getter
public class QueryDSL extends SQLQueryFactory {
    private final Jdbc jdbc;

    private QueryDSL(Configuration configuration, Supplier<Connection> connProvider, Jdbc jdbc) {
        super(configuration, connProvider);
        this.jdbc = jdbc;
    }

    public static QueryDSL create(Jdbc jdbc) {
        DataSource dataSource = jdbc.getJdbcTemplate().getJdbcTemplate().getDataSource();
        Assert.notNull(dataSource, "jdbc获取dataSource为空");
        DbType dbType = jdbc.getDbType();
        Configuration configuration = new Configuration(getSQLTemplates(dbType));
        SQLCoreListener sqlCoreListener = new SQLCoreListener(jdbc.getDbType(), jdbc.getJdbcTemplate(), jdbc.getListeners());
        configuration.addListener(sqlCoreListener);
        // configuration.addListener(new SQLRewriteListener());
        return new QueryDSL(configuration, sqlCoreListener.getConnProvider(), jdbc);
    }

    // --------------------------------------------------------------------------------------------
    // 常用操作封装
    // --------------------------------------------------------------------------------------------

    /**
     * 单表单条数据更新，只更新变化字段
     *
     * @param qTable       Q类
     * @param where        更新条件(只能查到一条数据)
     * @param data         更新的数据
     * @param updateBefore 执行更新之前的回调
     * @param ignoreFields 不需要更新的字段
     * @return false: 数据库里不存在原始数据。true: 更新成功或者数据库里的数据与data一致
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean updateChange(RelationalPath<?> qTable, Predicate where, Object data, Consumer<SQLUpdateClause> updateBefore, Path<?>... ignoreFields) {
        Assert.notNull(qTable, "参数 qTable 不能为 null");
        Assert.notNull(where, "参数 where 不能为 null");
        Assert.notNull(data, "参数 data 不能为 null");
        final List<Path<?>> columns = new ArrayList<>();
        for (Path<?> column : qTable.getColumns()) {
            if (ignoreFields != null && Arrays.asList(ignoreFields).contains(column)) {
                continue;
            }
            columns.add(column);
        }
        Assert.notEmpty(columns, "参数 ignoreFields 排除了所有字段");
        Tuple existsData = select(columns.toArray(new Expression[0])).from(qTable).where(where).fetchOne();
        if (existsData == null) {
            return false;
        }
        SQLUpdateClause update = update(qTable).where(where);
        Map<String, Object> dataMap = BeanCopyUtils.toMap(data);
        int changeCount = 0;
        for (Path column : columns) {
            Object existsValue = existsData.get(column);
            Object updateValue = dataMap.get(column.getMetadata().getName());
            if (Objects.equals(existsValue, updateValue)) {
                continue;
            }
            update.set(column, updateValue);
            changeCount++;
        }
        if (changeCount > 0) {
            if (updateBefore != null) {
                updateBefore.accept(update);
            }
            update.execute();
        }
        return true;
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

    /***
     * 批量获取唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param qClass QueryDSL Q类
     * @param size 唯一id值数量(1 ~ 10W)
     */
    public List<Long> nextIds(RelationalPathBase<?> qClass, int size) {
        return jdbc.nextIds(qClass.getTableName(), size);
    }

    /**
     * 返回下一个唯一的id值 <br/>
     * <b>此功能需要数据库表支持</b>
     *
     * @param qClass QueryDSL Q类
     */
    public Long nextId(RelationalPathBase<?> qClass) {
        return jdbc.nextId(qClass.getTableName());
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
    public void lock(String lockName, Runnable syncBlock) {
        jdbc.lock(lockName, syncBlock);
    }

    // --------------------------------------------------------------------------------------------
    //  内部函数
    // --------------------------------------------------------------------------------------------

    public static SQLTemplates getSQLTemplates(DbType dbType) {
        switch (dbType) {
            case MYSQL:
            case MARIADB:
                return MySQLTemplates.DEFAULT;
            case ORACLE:
                return OracleTemplates.DEFAULT;
            case DB2:
                return DB2Templates.DEFAULT;
            case H2:
                return H2Templates.DEFAULT;
            case HSQL:
                return HSQLDBTemplates.DEFAULT;
            case SQLITE:
                return SQLiteTemplates.DEFAULT;
            case POSTGRE_SQL:
                return PostgreSQLTemplates.DEFAULT;
            case SQL_SERVER2005:
                return SQLServer2005Templates.DEFAULT;
            case SQL_SERVER:
                return SQLServerTemplates.DEFAULT;
            default:
                throw new RuntimeException("不支持的数据库类型：" + dbType.getDb());
        }
    }
}
