package org.clever.data.jdbc;

import com.querydsl.core.QueryFlag;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.*;
import com.querydsl.sql.dml.Mapper;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLMergeClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import lombok.Getter;
import lombok.SneakyThrows;
import org.clever.core.Assert;
import org.clever.core.function.ZeroConsumer;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.querydsl.SQLCoreListener;
import org.clever.data.jdbc.querydsl.sql.OracleTemplates;
import org.clever.data.jdbc.querydsl.sql.PostgreSQLTemplates;
import org.clever.data.jdbc.querydsl.sql.SQLQueryFactory;
import org.clever.data.jdbc.querydsl.sql.dml.MapMapper;
import org.clever.data.jdbc.support.JdbcDataSourceStatus;
import org.clever.data.jdbc.support.JdbcInfo;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
    public boolean update(RelationalPath<?> qTable, Predicate where, Object data, Consumer<SQLUpdateClause> updateBefore, Path<?>... ignoreFields) {
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

    /**
     * 单表单条数据更新，只更新变化字段
     *
     * @param qTable       Q类
     * @param where        更新条件(只能查到一条数据)
     * @param data         更新的数据
     * @param ignoreFields 不需要更新的字段
     * @return false: 数据库里不存在原始数据。true: 更新成功或者数据库里的数据与data一致
     */
    public boolean update(RelationalPath<?> qTable, Predicate where, Object data, Path<?>... ignoreFields) {
        return update(qTable, where, data, null, ignoreFields);
    }

    /**
     * 新增或更新数据，数据不存在就 insert，数据存在就 update
     *
     * @param qTable     表对应的Q类
     * @param data       表数据实体对象
     * @param ignoreNull 是否忽略空值，不更新空值字段
     * @param keys       判断数据是否存在的字段
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public long merge(RelationalPath<?> qTable, Object data, boolean ignoreNull, Path<?>... keys) {
        SQLMergeClause merge = merge(qTable);
        Mapper<Map<String, ?>> mapper = ignoreNull ? MapMapper.WITH_NULL_BINDINGS : MapMapper.DEFAULT;
        Map<String, Object> dataMap = BeanCopyUtils.toMap(data);
        Map<Path<?>, Object> mapperMap = mapper.createMap(qTable, dataMap);
        for (Map.Entry<Path<?>, Object> entry : mapperMap.entrySet()) {
            merge.set((Path) entry.getKey(), entry.getValue());
        }
        merge.keys(keys);
        return merge.execute();
    }

    /**
     * 新增或更新数据，数据不存在就 insert，数据存在就 update，忽略空值字段
     *
     * @param qTable 表对应的Q类
     * @param data   表数据实体对象(忽略空值字段)
     * @param keys   判断数据是否存在的字段
     */
    public long merge(RelationalPath<?> qTable, Object data, Path<?>... keys) {
        return merge(qTable, data, true, keys);
    }

    /**
     * 新增或更新数据，利用目标数据库的方言语法实现
     *
     * @param qTable     表对应的Q类
     * @param data       表数据实体对象
     * @param ignoreNull 是否忽略空值，不更新空值字段
     * @param keys       判断数据是否存在的字段
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public long upsert(RelationalPath<?> qTable, Object data, boolean ignoreNull, Path<?>... keys) {
        // 生成正常的 insert 语句
        SQLInsertClause insert = insert(qTable);
        Map<String, Object> dataMap = BeanCopyUtils.toMap(data);
        Mapper<Map<String, ?>> mapper = ignoreNull ? MapMapper.WITH_NULL_BINDINGS : MapMapper.DEFAULT;
        Map<Path<?>, Object> mapperMap = mapper.createMap(qTable, dataMap);
        for (Map.Entry<Path<?>, Object> entry : mapperMap.entrySet()) {
            insert.set((Path) entry.getKey(), entry.getValue());
        }
        // 生成 “on duplicate key update” 或者 “on conflict” 部分
        List<Expression<?>> expressions = new ArrayList<>(mapperMap.size());
        switch (jdbc.getDbType()) {
            case MYSQL: {
                // insert into users (id, name, email) values (1, 'alice updated', 'alice.updated@example.com')
                // on duplicate key update name = values(name), email = values(email)
                expressions.add(Expressions.stringTemplate(" on duplicate key update"));
                int idx = 0;
                for (Map.Entry<Path<?>, Object> entry : mapperMap.entrySet()) {
                    idx++;
                    Path<?> column = entry.getKey();
                    Expression<?> expression;
                    if (idx < mapperMap.size()) {
                        expression = Expressions.stringTemplate(" {0} = values({0}),", column);
                    } else {
                        expression = Expressions.stringTemplate(" {0} = values({0})", column);
                    }
                    expressions.add(expression);
                }
            }
            break;
            case POSTGRE_SQL: {
                // insert into users (id, name, email) values (1, 'alice updated', 'alice.updated@example.com')
                // on conflict (id) do update set name = excluded.name, email = excluded.email
                Assert.notEmpty(keys, "参数 keys 不能为空");
                expressions.add(Expressions.stringTemplate(" on conflict ("));
                int idx = 0;
                for (Path<?> key : keys) {
                    idx++;
                    Expression<?> expression;
                    Expression<?> field = Expressions.simplePath(Void.class, key.getMetadata().getName());
                    if (idx < keys.length) {
                        if (idx == 1) {
                            expression = Expressions.stringTemplate("{0},", field);
                        } else {
                            expression = Expressions.stringTemplate(" {0},", field);
                        }
                    } else {
                        expression = Expressions.stringTemplate(" {0})", field);
                    }
                    expressions.add(expression);
                }
                expressions.add(Expressions.stringTemplate(" do update set"));
                idx = 0;
                for (Map.Entry<Path<?>, Object> entry : mapperMap.entrySet()) {
                    idx++;
                    Path<?> column = entry.getKey();
                    Expression<?> expression;
                    Expression<?> field = Expressions.simplePath(Void.class, column.getMetadata().getName());
                    if (idx < mapperMap.size()) {
                        expression = Expressions.stringTemplate(" {0} = excluded.{0},", field);
                    } else {
                        expression = Expressions.stringTemplate(" {0} = excluded.{0}", field);
                    }
                    expressions.add(expression);
                }
            }
            break;
            default:
                throw new RuntimeException("saveOrUpdate不支持数据库");
        }
        for (Expression<?> expression : expressions) {
            insert.addFlag(QueryFlag.Position.END, expression);
        }
        return insert.execute();
    }

    /**
     * 新增或更新数据，利用目标数据库的方言语法实现，忽略空值字段
     *
     * @param qTable 表对应的Q类
     * @param data   表数据实体对象(忽略空值字段)
     * @param keys   判断数据是否存在的字段
     */
    public long upsert(RelationalPath<?> qTable, Object data, Path<?>... keys) {
        return upsert(qTable, data, true, keys);
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
     * @see org.springframework.transaction.TransactionDefinition
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
     * @see org.springframework.transaction.TransactionDefinition
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

    /**
     * 处理 like 匹配时, 使用转义字符, 转义'%'、'_'通配符, 如:
     * <pre>
     * "abcdefg"        ->  "abcdefg"
     * "abc%de_fg"      ->  "abc\%de\_fg"
     * "abc\%de\_fg"    ->  "abc\%de\_fg"
     * "abc\%de_fg"     ->  "abc\%de\_fg"
     * </pre>
     * sql语句类似 “ and field like likeEscape(likeVal, escape) escape '\' ”
     *
     * @param field   like匹配字段
     * @param likeVal like匹配值
     * @param escape  转义字符，如{@code "\"}
     * @param prefix  是否前缀匹配
     * @param suffix  是否后缀匹配
     */
    public BooleanExpression likeMatch(StringPath field, String likeVal, String escape, boolean prefix, boolean suffix) {
        String likeEscape = jdbc.likeEscape(likeVal, escape);
        if (prefix) {
            likeEscape = "%" + likeEscape;
        }
        if (suffix) {
            likeEscape = likeEscape + "%";
        }
        return Expressions.booleanTemplate(
            " {0} like {1} escape {2} ",
            field,
            Expressions.constant(likeEscape),
            Expressions.constant(escape)
        );
    }

    /**
     * 生成 like 前缀匹配值, 转义'%'、'_'通配符
     * <pre>
     *  MySQL       -> escape '\\'(或者不写)
     *  PostgreSQL  -> escape '\' (或者不写)
     *  Oracle      -> escape '\'
     *  others      -> escape '\' (不能保证正确)
     * </pre>
     *
     * @param field   like匹配字段
     * @param likeVal like匹配值
     * @param prefix  是否前缀匹配
     * @param suffix  是否后缀匹配
     */
    public BooleanExpression likeMatch(StringPath field, String likeVal, boolean prefix, boolean suffix) {
        String escape = jdbc.getEscape();
        return likeMatch(field, likeVal, escape, prefix, suffix);
    }

    /**
     * 生成 like 前缀匹配值, 转义'%'、'_'通配符
     * <pre>
     *  MySQL       -> escape '\\'(或者不写)
     *  PostgreSQL  -> escape '\' (或者不写)
     *  Oracle      -> escape '\'
     *  others      -> escape '\' (不能保证正确)
     * </pre>
     */
    public BooleanExpression likePrefix(StringPath field, String likeVal) {
        return likeMatch(field, likeVal, true, false);
    }

    /**
     * 生成 like 后缀匹配值, 转义'%'、'_'通配符
     * <pre>
     *  MySQL       -> escape '\\'(或者不写)
     *  PostgreSQL  -> escape '\' (或者不写)
     *  Oracle      -> escape '\'
     *  others      -> escape '\' (不能保证正确)
     * </pre>
     */
    public BooleanExpression likeSuffix(StringPath field, String likeVal) {
        return likeMatch(field, likeVal, false, true);
    }

    /**
     * 生成 like 前缀和后缀匹配值, 转义'%'、'_'通配符
     * <pre>
     *  MySQL       -> escape '\\'(或者不写)
     *  PostgreSQL  -> escape '\' (或者不写)
     *  Oracle      -> escape '\'
     *  others      -> escape '\' (不能保证正确)
     * </pre>
     */
    public BooleanExpression likeBoth(StringPath field, String likeVal) {
        return likeMatch(field, likeVal, true, true);
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
    //  内部函数
    // --------------------------------------------------------------------------------------------

    public static SQLTemplates getSQLTemplates(DbType dbType) {
        return switch (dbType) {
            case MYSQL, MARIADB -> MySQLTemplates.DEFAULT;
            case ORACLE -> OracleTemplates.DEFAULT;
            case DB2 -> DB2Templates.DEFAULT;
            case H2 -> H2Templates.DEFAULT;
            case HSQL -> HSQLDBTemplates.DEFAULT;
            case SQLITE -> SQLiteTemplates.DEFAULT;
            case POSTGRE_SQL -> PostgreSQLTemplates.DEFAULT;
            case SQL_SERVER2005 -> SQLServer2005Templates.DEFAULT;
            case SQL_SERVER -> SQLServerTemplates.DEFAULT;
            default -> throw new RuntimeException("不支持的数据库类型：" + dbType.getDb());
        };
    }
}
