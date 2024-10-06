package org.clever.data.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.clever.core.AppShutdownHook;
import org.clever.core.Assert;
import org.clever.core.OrderIncrement;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.support.JdbcDataSourceStatus;
import org.clever.data.jdbc.support.JdbcInfo;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据源管理器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/03 19:26 <br/>
 */
public class DataSourceAdmin {
    /**
     * 默认的数据源
     */
    private static String DEFAULT_DATA_SOURCE_NAME;
    /**
     * 数据源集合 {@code ConcurrentMap<dataSourceName, DataSource>}
     */
    private static final ConcurrentMap<String, DataSource> DATASOURCE_MAP = new ConcurrentHashMap<>();
    /**
     * Jdbc集合 {@code ConcurrentMap<dataSourceName, Jdbc>}
     */
    private static final ConcurrentMap<String, Jdbc> JDBC_MAP = new ConcurrentHashMap<>();
    /**
     * 项目列表
     */
    private static List<String> PROJECTS = new ArrayList<>();
    /**
     * 全局的 MyBatisMapperSql
     */
    private static MyBatisMapperSql MYBATIS_MAPPER_SQL;
    /**
     * QueryDSL查询对象集合 {@code ConcurrentMap<dataSourceName, SQLQueryFactory>}
     */
    private static final ConcurrentMap<String, QueryDSL> DSL_FACTORY_MAP = new ConcurrentHashMap<>();

    static {
        AppShutdownHook.addShutdownHook(DataSourceAdmin::closeAllDataSource, OrderIncrement.MAX, "关闭数据库连接池");
    }

    /**
     * 关闭所有的数据源
     */
    public static void closeAllDataSource() {
        for (DataSource dataSource : DATASOURCE_MAP.values()) {
            if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
                continue;
            }
            try {
                if (!hikariDataSource.isClosed()) {
                    hikariDataSource.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 设置默认数据源
     *
     * @param defaultDataSourceName 默认数据源名称
     */
    public static void setDefaultDataSourceName(String defaultDataSourceName) {
        Assert.hasText(defaultDataSourceName, "参数defaultDataSourceName不能为空");
        DEFAULT_DATA_SOURCE_NAME = defaultDataSourceName;
    }

    /**
     * 默认数据源名称
     */
    public static String getDefaultDataSourceName() {
        return DEFAULT_DATA_SOURCE_NAME;
    }

    /**
     * 新增数据源
     *
     * @param dataSourceName 数据源名称
     * @param dataSource     数据源
     */
    public static void addDataSource(String dataSourceName, DataSource dataSource) {
        Assert.hasText(dataSourceName, "参数dataSourceName不能为空");
        Assert.isTrue(dataSource != null, "参数dataSource不能为空");
        Jdbc jdbc = new Jdbc(dataSourceName, dataSource);
        DATASOURCE_MAP.put(dataSourceName, dataSource);
        JDBC_MAP.put(dataSourceName, jdbc);
    }

    /**
     * 新增数据源
     *
     * @param dataSourceName 数据源名称
     * @param hikariConfig   数据源
     */
    public static void addDataSource(String dataSourceName, HikariConfig hikariConfig) {
        DataSource dataSource = new HikariDataSource(hikariConfig);
        addDataSource(dataSourceName, dataSource);
    }

    /**
     * 根据名称获取数据源
     *
     * @param dataSourceName 数据源名称
     */
    public static DataSource getDataSource(String dataSourceName) {
        return DATASOURCE_MAP.get(dataSourceName);
    }

    /**
     * 获取默认的Jdbc
     */
    public static DataSource getDefaultDataSource() {
        return getDataSource(DEFAULT_DATA_SOURCE_NAME);
    }

    /**
     * 根据名称获取数据源
     *
     * @param dataSourceName 数据源名称
     */
    public static Jdbc getJdbc(String dataSourceName) {
        return JDBC_MAP.get(dataSourceName);
    }

    /**
     * 获取默认的Jdbc
     */
    public static Jdbc getDefaultJdbc() {
        return getJdbc(DEFAULT_DATA_SOURCE_NAME);
    }

    /**
     * 获取QueryDSL
     */
    public static QueryDSL getQueryDSL(String dataSourceName) {
        final Jdbc jdbc = DataSourceAdmin.getJdbc(dataSourceName);
        if (jdbc == null) {
            return null;
        }
        return DSL_FACTORY_MAP.computeIfAbsent(dataSourceName, name -> QueryDSL.create(jdbc));
    }

    /**
     * 获取默认的QueryDSL
     */
    public static QueryDSL getDefaultQueryDSL() {
        return getQueryDSL(DEFAULT_DATA_SOURCE_NAME);
    }

    /**
     * 设置默认的项目列表
     */
    public static void setProjects(List<String> projects) {
        if (projects == null) {
            projects = new ArrayList<>();
        }
        PROJECTS = projects;
    }

    /**
     * 默认的项目列表
     */
    public static List<String> getProjects() {
        return Collections.unmodifiableList(PROJECTS);
    }

    /**
     * 设置全局的 MyBatisMapperSql
     */
    public static void setMyBatisMapperSql(MyBatisMapperSql mybatisMapperSql) {
        Assert.notNull(mybatisMapperSql, "参数 mybatisMapperSql 不能为 null");
        MYBATIS_MAPPER_SQL = mybatisMapperSql;
    }

    /**
     * 获取全局的 MyBatisMapperSql
     */
    public static MyBatisMapperSql getMyBatisMapperSql() {
        return MYBATIS_MAPPER_SQL;
    }

    /**
     * 获取所有数据源名称
     */
    public static Set<String> allDatasourceNames() {
        return JDBC_MAP.keySet();
    }

    /**
     * 获取数据源信息
     *
     * @param name 数据源名称
     */
    public static JdbcInfo getInfo(String name) {
        Jdbc jdbcDataSource = getJdbc(name);
        return jdbcDataSource == null ? null : jdbcDataSource.getInfo();
    }

    /**
     * 获取所有数据源信息
     */
    public static Map<String, JdbcInfo> allInfos() {
        Map<String, JdbcInfo> map = new HashMap<>(JDBC_MAP.size());
        for (Map.Entry<String, Jdbc> entry : JDBC_MAP.entrySet()) {
            String name = entry.getKey();
            map.put(name, getInfo(name));
        }
        return map;
    }

    /**
     * 获取数据源状态
     *
     * @param name 数据源名称
     */
    public static JdbcDataSourceStatus getStatus(String name) {
        Jdbc jdbcDataSource = getJdbc(name);
        return jdbcDataSource == null ? null : jdbcDataSource.getStatus();
    }

    /**
     * 获取数据源状态
     */
    public static Map<String, JdbcDataSourceStatus> allStatus() {
        Map<String, JdbcDataSourceStatus> map = new HashMap<>(JDBC_MAP.size());
        for (Map.Entry<String, Jdbc> entry : JDBC_MAP.entrySet()) {
            String name = entry.getKey();
            map.put(name, getStatus(name));
        }
        return map;
    }
}
