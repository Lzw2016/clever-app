package org.clever.data.jdbc.meta;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.QuerySyncState;
import org.clever.data.jdbc.meta.model.Table;
import org.clever.data.jdbc.meta.model.TablesSyncState;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/21 18:15 <br/>
 */
@Slf4j
public class DataSyncJobTest {
    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t01() {
        Jdbc mysql = BaseTest.newMysql();
        Jdbc oracle = BaseTest.newOracle();
        MySQLMetaData mySQLMeta = new MySQLMetaData(mysql);
        OracleMetaData oracleMeta = new OracleMetaData(oracle);
        Table table = oracleMeta.getTable("WMS8DEV", "BAS_ITEM");
        log.info("--> \n{}", mySQLMeta.createTable(table));
        TablesSyncState syncState = DataSyncJob.tableSync(oracle, mysql, false, false, "bas_item");
        while (syncState.getSuccess() == null) {
            log.info("State={}", syncState);
            Thread.sleep(1000);
        }
        log.info("###完成 -> {}", syncState);
        mysql.close();
        oracle.close();
    }

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t02() {
        Jdbc mysql = BaseTest.newMysql();
        Jdbc oracle = BaseTest.newOracle();
        QuerySyncState syncState = DataSyncJob.querySync(oracle, mysql, false, "select * from bas_item where STOCK_ENV=11", "bas_item");
        while (syncState.getSuccess() == null) {
            log.info("State={}", syncState);
            Thread.sleep(1000);
        }
        log.info("###完成 -> {}", syncState);
        mysql.close();
        oracle.close();
    }
}
