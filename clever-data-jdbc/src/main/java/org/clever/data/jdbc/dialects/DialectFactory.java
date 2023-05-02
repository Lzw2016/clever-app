package org.clever.data.jdbc.dialects;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.model.request.page.IPage;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.support.mybatisplus.ExceptionUtils;
import org.clever.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者： lzw<br/>
 * 创建时间：2019-10-03 12:20 <br/>
 */
public class DialectFactory {
    /**
     * 方言缓存 {@code Map<DbType.getDb(), IDialect>}
     */
    private static final Map<String, IDialect> DIALECT_CACHE = new ConcurrentHashMap<>();

    /**
     * 设置数据库对应的 IDialect
     *
     * @param dbType  数据库类型
     * @param dialect 数据库方言
     */
    public static void setDialect(DbType dbType, IDialect dialect) {
        Assert.notNull(dbType, "参数dbType不能为null");
        Assert.notNull(dialect, "参数dialect不能为null");
        DIALECT_CACHE.put(dbType.getDb(), dialect);
        DIALECT_CACHE.put(dialect.getClass().getName(), dialect);
    }

    /**
     * 生成带分页的sql语句
     *
     * @param offset       数据偏移量
     * @param limit        数据量
     * @param buildSql     原始 SQL
     * @param paramMap     Sql参数
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     * @return 分页模型
     */
    public static String buildPaginationSql(long offset, long limit, String buildSql, Map<String, Object> paramMap, DbType dbType, String dialectClazz) {
        return getDialect(dbType, dialectClazz).buildPaginationSql(buildSql, offset, limit, paramMap);
    }

    /**
     * 生成带分页的sql语句
     *
     * @param offset       数据偏移量
     * @param limit        数据量
     * @param buildSql     原始 SQL
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     */
    public static String buildPaginationSql(long offset, long limit, String buildSql, DbType dbType, String dialectClazz) {
        return getDialect(dbType, dialectClazz).buildPaginationSql(buildSql, offset, limit);
    }

    /**
     * 生成带分页的sql语句
     *
     * @param page         分页对象
     * @param buildSql     原始 SQL
     * @param paramMap     Sql参数
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     * @return 分页模型
     */
    public static String buildPaginationSql(IPage<?> page, String buildSql, Map<String, Object> paramMap, DbType dbType, String dialectClazz) {
        return buildPaginationSql(page.offset(), page.getSize(), buildSql, paramMap, dbType, dialectClazz);
    }

    /**
     * 生成带分页的sql语句
     *
     * @param page         分页对象
     * @param buildSql     原始 SQL
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     */
    public static String buildPaginationSql(IPage<?> page, String buildSql, DbType dbType, String dialectClazz) {
        return buildPaginationSql(page.offset(), page.getSize(), buildSql, dbType, dialectClazz);
    }

    /**
     * 获取查询数据库当前时间的sql和参数
     *
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     */
    public static TupleTwo<String, Map<String, Object>> currentDateTimeSql(DbType dbType, String dialectClazz) {
        return getDialect(dbType, dialectClazz).currentDateTimeSql();
    }

    /**
     * 获取查询当前序列值的sql和参数
     *
     * @param seqName      序列名称
     * @param dbType       数据库类型
     * @param dialectClazz 数据库方言
     */
    public static TupleTwo<String, Map<String, Object>> nextSeqSql(String seqName, DbType dbType, String dialectClazz) {
        return getDialect(dbType, dialectClazz).nextSeqSql(seqName);
    }

    /**
     * 获取数据库方言
     *
     * @param dbType       数据库类型
     * @param dialectClazz 自定义方言实现类
     * @return ignore
     */
    public static IDialect getDialect(DbType dbType, String dialectClazz) {
        IDialect dialect = DIALECT_CACHE.get(dbType.getDb());
        if (null == dialect) {
            // 自定义方言
            if (StringUtils.isNotBlank(dialectClazz)) {
                dialect = DIALECT_CACHE.get(dialectClazz);
                if (null != dialect) {
                    return dialect;
                }
                try {
                    Class<?> clazz = Class.forName(dialectClazz);
                    if (IDialect.class.isAssignableFrom(clazz)) {
                        dialect = (IDialect) clazz.newInstance();
                        DIALECT_CACHE.put(dialectClazz, dialect);
                    }
                } catch (ClassNotFoundException e) {
                    throw ExceptionUtils.mpe("Class : %s is not found", dialectClazz);
                } catch (IllegalAccessException | InstantiationException e) {
                    throw ExceptionUtils.mpe("Class : %s can not be instance", dialectClazz);
                }
            } else {
                // 缓存方言
                dialect = getDialectByDbType(dbType);
                DIALECT_CACHE.put(dbType.getDb(), dialect);
            }
            /* 未配置方言则抛出异常 */
            Assert.notNull(dialect, "The value of the dialect property in mybatis configuration.xml is not defined.");
        }
        return dialect;
    }

    /**
     * 根据数据库类型选择不同分页方言
     *
     * @param dbType 数据库类型
     * @return 分页语句组装类
     */
    private static IDialect getDialectByDbType(DbType dbType) {
        switch (dbType) {
            case MYSQL:
            case MARIADB:
            case CLICK_HOUSE:
            case OCEAN_BASE:
                return new MySqlDialect();
            case ORACLE:
            case DM:
                return new OracleDialect();
            case ORACLE_12C:
                return new Oracle12cDialect();
            case DB2:
                return new DB2Dialect();
            case H2:
                return new H2Dialect();
            case HSQL:
                return new HSQLDialect();
            case SQLITE:
                return new SQLiteDialect();
            case POSTGRE_SQL:
            case PHOENIX:
                return new PostgreDialect();
            case SQL_SERVER2005:
                return new SQLServer2005Dialect();
            case SQL_SERVER:
                return new SQLServerDialect();
            default:
                throw ExceptionUtils.mpe("%s database not supported.", dbType.getDb());
        }
    }
}
