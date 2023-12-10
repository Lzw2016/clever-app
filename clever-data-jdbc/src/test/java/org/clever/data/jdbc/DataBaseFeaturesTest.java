package org.clever.data.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.support.features.DataBaseFeatures;
import org.clever.data.jdbc.support.features.MySQLFeatures;
import org.clever.data.jdbc.support.features.OracleFeatures;
import org.clever.data.jdbc.support.features.PostgreSQLFeatures;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/10 13:19 <br/>
 */
@Slf4j
public class DataBaseFeaturesTest {
    private final String lockName = "000123456";

    @SneakyThrows
    private void newThreadGetLock(DataBaseFeatures features) {
        Thread thread = new Thread(() -> {
            features.getJdbc().beginTX(status -> {
                boolean res = features.getLock(lockName, 1);
                log.info("#Thread\tgetLock={}", res);
            });
        });
        thread.setName("test");
        thread.start();
        thread.join();
    }

    public void check(DataBaseFeatures features) {
        final int count = 3;
        features.getJdbc().beginTX(status -> {
            boolean res;
            // 锁多次
            for (int i = 0; i < count; i++) {
                res = features.getLock(lockName);
                log.info("#{}\t\t\tgetLock={}", i, res);
            }
            // 释放多次
            for (int i = 0; i < count; i++) {
                res = features.releaseLock(lockName);
                log.info("#{}\t\t\treleaseLock={}", i, res);
                // 尝试获取锁
                newThreadGetLock(features);
            }
            // 过多释放
            for (int i = 0; i < count; i++) {
                res = features.releaseLock(lockName);
                log.info("#{}\t\t\treleaseLock={}", i, res);
            }
        });
    }

    @Test
    public void t01() {
        Jdbc mysql = BaseTest.newMysql();
        MySQLFeatures features = new MySQLFeatures(mysql);
        check(features);
        mysql.close();
    }

    @Test
    public void t02() {
        Jdbc postgresql = BaseTest.newPostgresql();
        PostgreSQLFeatures features = new PostgreSQLFeatures(postgresql);
        check(features);
        postgresql.close();
    }

    @Test
    public void t03() {
        Jdbc oracle = BaseTest.newOracle();
        OracleFeatures features = new OracleFeatures(oracle);
        check(features);
        oracle.close();
    }
}
