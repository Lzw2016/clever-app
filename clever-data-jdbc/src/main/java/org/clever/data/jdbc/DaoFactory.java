package org.clever.data.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/03 19:13 <br/>
 */
public class DaoFactory {
    /**
     * 获取Jdbc查询对象
     *
     * @param dataSourceName 数据源名称
     */
    public static Jdbc getJdbc(String dataSourceName) {
        return DataSourceAdmin.getJdbc(dataSourceName);
    }

    /**
     * 获取Jdbc查询对象
     */
    public static Jdbc getJdbc() {
        return DataSourceAdmin.getDefaultJdbc();
    }

    /**
     * 获取QueryDSL查询对象
     *
     * @param dataSourceName 数据源名称
     */
    public static QueryDSL getQueryDSL(String dataSourceName) {
        return DataSourceAdmin.getQueryDSL(dataSourceName);
    }

    /**
     * 获取QueryDSL查询对象
     */
    public static QueryDSL getQueryDSL() {
        return DataSourceAdmin.getDefaultQueryDSL();
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param stdXmlPath     标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
     * @param dataSourceName 数据源名称
     * @param projects       项目列表
     */
    public static MyBatis getMyBatis(String stdXmlPath, String dataSourceName, List<String> projects) {
        Jdbc jdbc = DataSourceAdmin.getJdbc(dataSourceName);
        Collections.reverse(projects);
        MyBatisMapperSql mapperSql = DataSourceAdmin.getMyBatisMapperSql();
        return new MyBatis(jdbc, stdXmlPath, projects, mapperSql);
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param stdXmlPath     标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
     * @param dataSourceName 数据源名称
     */
    public static MyBatis getMyBatis(String stdXmlPath, String dataSourceName) {
        return getMyBatis(stdXmlPath, dataSourceName, DataSourceAdmin.getProjects());
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param stdXmlPath 标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
     */
    public static MyBatis getMyBatis(String stdXmlPath) {
        return getMyBatis(
            stdXmlPath,
            DataSourceAdmin.getDefaultDataSourceName(),
            DataSourceAdmin.getProjects()
        );
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param clazz          SQL文件基础路径对应的class
     * @param dataSourceName 数据源名称
     * @param projects       项目列表
     */
    public static MyBatis getMyBatis(Class<?> clazz, String dataSourceName, List<String> projects) {
        String stdXmlPath = StringUtils.replace(clazz.getName(), ".", "/") + ".xml";
        return getMyBatis(stdXmlPath, dataSourceName, projects);
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param clazz          SQL文件基础路径对应的class
     * @param dataSourceName 数据源名称
     */
    public static MyBatis getMyBatis(Class<?> clazz, String dataSourceName) {
        return getMyBatis(clazz, dataSourceName, DataSourceAdmin.getProjects());
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param clazz SQL文件基础路径对应的class
     */
    public static MyBatis getMyBatis(Class<?> clazz) {
        return getMyBatis(
            clazz,
            DataSourceAdmin.getDefaultDataSourceName(),
            DataSourceAdmin.getProjects()
        );
    }

    /**
     * 获取 MyBatis Mapper 对象实例 <br/>
     *
     * @param mapper         Mapper接口
     * @param dataSourceName 数据源名称
     * @param projects       项目列表
     * @param <T>            Mapper接口类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMapper(Class<?> mapper, String dataSourceName, List<String> projects) {
        Assert.notNull(mapper, "参数 mapper 不能为 null");
        Assert.isNotBlank(dataSourceName, "参数 dataSourceName 不能为空");
        Jdbc jdbc = DataSourceAdmin.getJdbc(dataSourceName);
        Collections.reverse(projects);
        MyBatisMapperSql mapperSql = DataSourceAdmin.getMyBatisMapperSql();
        return (T) Proxy.newProxyInstance(
            mapper.getClassLoader(),
            new Class[]{mapper},
            new MyBatisMapperHandler(mapper, projects, mapperSql, jdbc)
        );
    }

    /**
     * 获取 MyBatis Mapper 对象实例 <br/>
     *
     * @param mapper         Mapper接口
     * @param dataSourceName 数据源名称
     * @param <T>            Mapper接口类型
     */
    public static <T> T getMapper(Class<?> mapper, String dataSourceName) {
        return getMapper(
            mapper,
            dataSourceName,
            DataSourceAdmin.getProjects()
        );
    }

    /**
     * 获取 MyBatis Mapper 对象实例 <br/>
     *
     * @param mapper Mapper接口
     * @param <T>    Mapper接口类型
     */
    public static <T> T getMapper(Class<?> mapper) {
        return getMapper(
            mapper,
            DataSourceAdmin.getDefaultDataSourceName(),
            DataSourceAdmin.getProjects()
        );
    }
}
