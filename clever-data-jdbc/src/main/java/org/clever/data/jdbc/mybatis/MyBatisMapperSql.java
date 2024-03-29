package org.clever.data.jdbc.mybatis;

import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/30 15:28 <br/>
 */
public interface MyBatisMapperSql {
    /**
     * 获取 SqlSource
     *
     * @param sqlId      SQL ID
     * @param stdXmlPath 标准的sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”
     * @param dbType     数据库类型
     * @param projects   项目列表(优选级由高到底)
     * @return 不存在返回null
     */
    SqlSource getSqlSource(String sqlId, String stdXmlPath, DbType dbType, String... projects);

    /**
     * 重新加载指定文件(文件被删除或者文件更新之后调用)
     *
     * @param xmlPath       sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     * @param skipException 是否跳过异常
     */
    void reloadFile(final String xmlPath, boolean skipException);

    /**
     * 加载所有文件
     */
    void reloadAll();

    /**
     * 开始监听sql.xml文件变化
     *
     * @param period 两次执行任务的时间间隔(单位：毫秒)
     */
    void startWatch(long period);

    /**
     * 停止监听sql.xml文件变化
     */
    void stopWatch();

    /**
     * 是否在监听sql.xml文件变化
     */
    boolean isWatch();
}
