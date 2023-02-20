package org.clever.security;


import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.DaoFactory;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.data.redis.RedisAdmin;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/30 19:10 <br/>
 */
public class SecurityDataSource {
    /**
     * security 模块使用的 jdbc 数据源
     */
    public static String JDBC_DATA_SOURCE_NAME;
    /**
     * security 模块使用的 redis 数据源
     */
    public static String REDIS_DATA_SOURCE_NAME;

    public static Jdbc getJdbc() {
        if (StringUtils.isBlank(JDBC_DATA_SOURCE_NAME)) {
            return DaoFactory.getJdbc();
        } else {
            return DaoFactory.getJdbc(JDBC_DATA_SOURCE_NAME);
        }
    }

    public static QueryDSL getQueryDSL() {
        if (StringUtils.isBlank(JDBC_DATA_SOURCE_NAME)) {
            return DaoFactory.getQueryDSL();
        } else {
            return DaoFactory.getQueryDSL(JDBC_DATA_SOURCE_NAME);
        }
    }

    public static Redis getRedis() {
        if (StringUtils.isBlank(REDIS_DATA_SOURCE_NAME)) {
            return RedisAdmin.getRedis();
        } else {
            return RedisAdmin.getRedis(REDIS_DATA_SOURCE_NAME);
        }
    }
}
