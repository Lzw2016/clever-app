package org.clever.data.jdbc;

import com.zaxxer.hikari.HikariConfig;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/12 21:39 <br/>
 */
@Slf4j
public class BaseTest {
    public static HikariConfig mysqlConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://192.168.1.201:30011/test");
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
        hikariConfig.setJdbcUrl("jdbc:postgresql://192.168.1.201:30010/test");
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

    public static FileSystemMyBatisMapperSql newFileSystemMyBatisMapperSql(String rootPath, String filter) {
        // final String rootPath = new File("./src/test/resources").getAbsolutePath();
        FileSystemMyBatisMapperSql myBatisMapperSql = new FileSystemMyBatisMapperSql(rootPath, filter);
        myBatisMapperSql.reloadAll();
        myBatisMapperSql.startWatch(200);
        log.info("sqlSourceCount->{}", myBatisMapperSql.getSqlSourceCount());
        return myBatisMapperSql;
    }
}
