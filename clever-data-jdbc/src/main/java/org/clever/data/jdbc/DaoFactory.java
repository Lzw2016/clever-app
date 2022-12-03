package org.clever.data.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;

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
     * @param mapperSqlName  MyBatisMapperSql名称
     * @param projects       项目列表
     */
    public static MyBatis getMyBatis(String stdXmlPath, String dataSourceName, String mapperSqlName, List<String> projects) {
        Jdbc jdbc = DataSourceAdmin.getJdbc(dataSourceName);
        Collections.reverse(projects);
        MyBatisMapperSql mapperSql = DataSourceAdmin.getMyBatisMapperSql(mapperSqlName);
        return new MyBatis(jdbc, stdXmlPath, projects, mapperSql);
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param stdXmlPath     标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
     * @param dataSourceName 数据源名称
     */
    public static MyBatis getMyBatis(String stdXmlPath, String dataSourceName) {
        return getMyBatis(stdXmlPath, dataSourceName, DataSourceAdmin.getDefaultMapperSqlName(), DataSourceAdmin.getDefaultProjects());
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
                DataSourceAdmin.getDefaultMapperSqlName(),
                DataSourceAdmin.getDefaultProjects()
        );
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param clazz          SQL文件基础路径对应的class
     * @param dataSourceName 数据源名称
     * @param mapperSqlName  MyBatisMapperSql名称
     * @param projects       项目列表
     */
    public static MyBatis getMyBatis(Class<?> clazz, String dataSourceName, String mapperSqlName, List<String> projects) {
        String stdXmlPath = StringUtils.replace(clazz.getName(), ".", "/") + ".xml";
        return getMyBatis(stdXmlPath, dataSourceName, mapperSqlName, projects);
    }

    /**
     * 创建新的 MyBatis 对象
     *
     * @param clazz          SQL文件基础路径对应的class
     * @param dataSourceName 数据源名称
     */
    public static MyBatis getMyBatis(Class<?> clazz, String dataSourceName) {
        return getMyBatis(clazz, dataSourceName, DataSourceAdmin.getDefaultMapperSqlName(), DataSourceAdmin.getDefaultProjects());
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
                DataSourceAdmin.getDefaultMapperSqlName(),
                DataSourceAdmin.getDefaultProjects()
        );
    }

//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param stdXmlPath     标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
//     * @param dataSourceName 数据源名称
//     * @param mapperSqlName  MyBatisMapperSql名称
//     * @param projects       项目列表
//     */
//    public static Dao getDao(String stdXmlPath, String dataSourceName, String mapperSqlName, List<String> projects) {
//        MyBatis myBatis = getMyBatis(stdXmlPath, dataSourceName, mapperSqlName, projects);
//        return new Dao(myBatis);
//    }
//
//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param stdXmlPath     标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
//     * @param dataSourceName 数据源名称
//     */
//    public static Dao getDao(String stdXmlPath, String dataSourceName) {
//        MyBatis myBatis = getMyBatis(stdXmlPath, dataSourceName, DataSourceAdmin.getDefaultMapperSqlName(), DataSourceAdmin.getDefaultProjects());
//        return new Dao(myBatis);
//    }
//
//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param stdXmlPath 标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
//     */
//    public static Dao getDao(String stdXmlPath) {
//        MyBatis myBatis = getMyBatis(
//                stdXmlPath,
//                DataSourceAdmin.getDefaultDataSourceName(),
//                DataSourceAdmin.getDefaultMapperSqlName(),
//                DataSourceAdmin.getDefaultProjects()
//        );
//        return new Dao(myBatis);
//    }
//
//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param clazz          SQL文件基础路径对应的class
//     * @param dataSourceName 数据源名称
//     * @param mapperSqlName  MyBatisMapperSql名称
//     * @param projects       项目列表
//     */
//    public static Dao getDao(Class<?> clazz, String dataSourceName, String mapperSqlName, List<String> projects) {
//        MyBatis myBatis = getMyBatis(clazz, dataSourceName, mapperSqlName, projects);
//        return new Dao(myBatis);
//    }
//
//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param clazz          SQL文件基础路径对应的class
//     * @param dataSourceName 数据源名称
//     */
//    public static Dao getDao(Class<?> clazz, String dataSourceName) {
//        MyBatis myBatis = getMyBatis(clazz, dataSourceName, DataSourceAdmin.getDefaultMapperSqlName(), DataSourceAdmin.getDefaultProjects());
//        return new Dao(myBatis);
//    }
//
//    /**
//     * 创建新的 Dao 对象
//     *
//     * @param clazz SQL文件基础路径对应的class
//     */
//    public static Dao getDao(Class<?> clazz) {
//        MyBatis myBatis = getMyBatis(
//                clazz,
//                DataSourceAdmin.getDefaultDataSourceName(),
//                DataSourceAdmin.getDefaultMapperSqlName(),
//                DataSourceAdmin.getDefaultProjects()
//        );
//        return new Dao(myBatis);
//    }
}