package org.clever.data.jdbc.meta;

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
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(512);
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
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(512);
        return hikariConfig;
    }

    public static Jdbc newPostgresql() {
        return new Jdbc(postgresqlConfig());
    }

    public static HikariConfig oracleConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("oracle.jdbc.OracleDriver");
        hikariConfig.setJdbcUrl("jdbc:oracle:thin:@122.9.140.63:1521:wms8dev");
        hikariConfig.setUsername("wms8dev");
        hikariConfig.setPassword("lmis9system");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(512);
        return hikariConfig;
    }

    public static Jdbc newOracle() {
        return new Jdbc(oracleConfig());
    }
}
