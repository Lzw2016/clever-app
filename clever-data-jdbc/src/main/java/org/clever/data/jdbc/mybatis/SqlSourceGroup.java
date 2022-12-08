package org.clever.data.jdbc.mybatis;

import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * mybatis动态sql文件组。例如:
 * <pre>{@code
 * - sql.xml
 * - sql.mysql.xml
 * - sql.oracle.xml
 * - sql.sqlserver.xml
 * - sql.projectA.xml
 * - sql.projectB.xml
 * - sql.projectC.xml
 * }</pre>
 * 作者：lizw <br/>
 * 创建时间：2021/12/02 20:43 <br/> unit
 */
public class SqlSourceGroup {
    /**
     * 标准的 sql.xml <br/>
     * {@code Map<sqlId, SqlSource>}
     */
    private final ConcurrentMap<String, SqlSource> stdSqlSource = new ConcurrentHashMap<>();
    /**
     * 项目的数据库方言 sql.projectA.xml <br/>
     * {@code Map<project, Map<sqlId, SqlSource>>}
     */
    private final ConcurrentMap<String, ConcurrentMap<String, SqlSource>> projectMap = new ConcurrentHashMap<>();
    /**
     * 数据库方言 sql.dbType.xml <br/>
     * {@code Map<dbType, Map<sqlId, SqlSource>>}
     */
    private final ConcurrentMap<String, ConcurrentMap<String, SqlSource>> dbTypeMap = new ConcurrentHashMap<>();

    /**
     * 根据优先级获取 SqlSource(项目优先级 > 数据库优先级)
     *
     * @param dbType   数据库类型
     * @param sqlId    SQL ID
     * @param projects 项目列表
     * @return 不存在返回null
     */
    public SqlSource getSqlSource(String sqlId, DbType dbType, String... projects) {
        if (projects == null) {
            projects = new String[0];
        }
        // 项目优先级
        for (String project : projects) {
            Map<String, SqlSource> sqlSourceMap = projectMap.get(project);
            if (sqlSourceMap != null) {
                SqlSource sqlSource = sqlSourceMap.get(sqlId);
                if (sqlSource != null) {
                    return sqlSource;
                }
            }
        }
        // 数据库优先级
        Map<String, SqlSource> sqlSourceMap = dbTypeMap.get(dbType.getDb());
        if (sqlSourceMap != null) {
            SqlSource sqlSource = sqlSourceMap.get(sqlId);
            if (sqlSource != null) {
                return sqlSource;
            }
        }
        // 标准SQL文件
        return stdSqlSource.get(sqlId);
    }

    public void clearAndSetStdSqlSource(Map<String, SqlSource> sqlSourceMap) {
        stdSqlSource.clear();
        stdSqlSource.putAll(sqlSourceMap);
    }

    public void clearStdSqlSource() {
        stdSqlSource.clear();
    }

    public void clearAndSetProjectMap(String project, Map<String, SqlSource> sqlSourceMap) {
        ConcurrentMap<String, SqlSource> map = projectMap.computeIfAbsent(project, key -> new ConcurrentHashMap<>());
        map.clear();
        map.putAll(sqlSourceMap);
    }

    public void removeProjectMap(String project) {
        projectMap.remove(project);
    }

    public void clearAndSetDbTypeMap(DbType dbType, Map<String, SqlSource> sqlSourceMap) {
        ConcurrentMap<String, SqlSource> map = dbTypeMap.computeIfAbsent(dbType.getDb(), db -> new ConcurrentHashMap<>());
        map.clear();
        map.putAll(sqlSourceMap);
    }

    public void removeDbTypeMap(DbType dbType) {
        dbTypeMap.remove(dbType.getDb());
    }

    /**
     * 返回当前 SqlSource 对象的数量
     */
    public int getSqlSourceCount() {
        int count = stdSqlSource.size();
        for (ConcurrentMap<String, SqlSource> item : projectMap.values()) {
            count = count + item.size();
        }
        for (ConcurrentMap<String, SqlSource> item : dbTypeMap.values()) {
            count = count + item.size();
        }
        return count;
    }
}
