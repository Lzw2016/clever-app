package org.clever.task.core;

import com.zaxxer.hikari.HikariConfig;
import org.clever.data.jdbc.Jdbc;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 09:57 <br/>
 */
public class BaseTest {
    public static HikariConfig mysqlConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://192.168.1.211:30019/test");
        hikariConfig.setUsername("admin");
        hikariConfig.setPassword("admin123456");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(128);
        hikariConfig.setMaximumPoolSize(1000);
        return hikariConfig;
    }

    public static Jdbc newMysql() {
        return new Jdbc(mysqlConfig());
    }

    public static HikariConfig postgresqlConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl("jdbc:postgresql://192.168.1.211:30010/test");
        hikariConfig.setUsername("admin");
        hikariConfig.setPassword("admin123456");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(100);
        hikariConfig.setMaximumPoolSize(512);
        return hikariConfig;
    }

    public static Jdbc newPostgresql() {
        return new Jdbc(postgresqlConfig());
    }
}
