package org.clever.data.jdbc.meta;

import com.zaxxer.hikari.HikariConfig;
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
    @Test
    public void t01() {
        Jdbc mysql = BaseTest.newMysql();
        Jdbc oracle = BaseTest.newOracle();
        MySQLMetaData mySQLMeta = new MySQLMetaData(mysql);
        OracleMetaData oracleMeta = new OracleMetaData(oracle);
        Table table = oracleMeta.getTable("WMS8DEV", "BAS_ITEM");
        log.info("--> \n{}", mySQLMeta.createTable(table));
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

    @SuppressWarnings("BusyWait")
    @SneakyThrows
    @Test
    public void t03() {
        Jdbc mysql = BaseTest.newMysql();
        Jdbc oracle = BaseTest.newOracle();
        TablesSyncState syncState = DataSyncJob.tableSync(oracle, mysql, false, false, "bas_item");
        while (syncState.getSuccess() == null) {
            log.info("State={}", syncState);
            Thread.sleep(1000);
        }
        log.info("###完成 -> {}", syncState);
        mysql.close();
        oracle.close();
    }

    @Test
    public void t04() {
        Jdbc postgresql = BaseTest.newPostgresql();
        Jdbc oracle = BaseTest.newOracle();
        String ddl = DataSyncJob.structSync(
            oracle,
            postgresql,
            "WMS8DEV",
            "public",
            false,
            "ASN_IN",
            "ASN_IN_DETAILS"
        );
        log.info("--> \n{}", ddl);
        postgresql.close();
        oracle.close();
    }

    @Test
    public void t05() {
        Jdbc mysql = BaseTest.newMysql();
        Jdbc oracle = BaseTest.newOracle();
        String ddl = DataSyncJob.structSync(
            mysql,
            oracle,
            "test",
            "WMSYS",
            false,
            "",
            "task_http_job",
            "task_java_job",
            "task_job",
            "task_job_log",
            "task_job_trigger",
            "task_job_trigger_log",
            "task_js_job",
            "task_scheduler",
            "task_scheduler_log",
            "task_shell_job",
            ""
        );
        log.info("--> \n{}", ddl);
        mysql.close();
        oracle.close();
    }

    @Test
    public void t06() {
        Jdbc mysql = BaseTest.newMysql();
        String ddl = DataSyncJob.structSync(
            mysql,
            mysql,
            "test",
            "test",
            false,
            "ASN_IN",
            "ASN_IN_DETAILS"
        );
        log.info("--> \n{}", ddl);
        mysql.close();
    }

    @Test
    public void t07() {
        Jdbc mysql = BaseTest.newMysql();
        String ddl = DataSyncJob.procedureDLL(
            mysql,
            "test",
            "next_id"
        );
        log.info("--> \n{}", ddl);
        mysql.close();
    }

    @SneakyThrows
    @Test
    public void t08() {
        Jdbc postgresql = BaseTest.newPostgresql();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl("jdbc:postgresql://192.168.1.211:5432/test");
        hikariConfig.setUsername("postgres");
        hikariConfig.setPassword("abc123ABC456");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(512);
        Jdbc jdbc2 = new Jdbc(hikariConfig);

//        String ddl = DataSyncJob.structSync(
//            postgresql,
//            jdbc2,
//            "public",
//            "public",
//            false,
//            "order_wave",
//            "order_wave_details",
//            "order_out",
//            "order_out_details"
//        );
//        log.info("###完成 -> \n{}", ddl);

        TablesSyncState state = DataSyncJob.tableSync(
            postgresql,
            jdbc2,
            false,
            false,
            "order_wave",
            "order_wave_details",
            "order_out",
            "order_out_details"
        );
        while (state.getSuccess() == null) {
            log.info("State={}", state);
            Thread.sleep(1000);
        }

        postgresql.close();
        jdbc2.close();
    }

//    @Test
//    public void t09() {
//        HikariConfig hikariConfig = new HikariConfig();
//        hikariConfig.setDriverClassName("org.postgresql.Driver");
//        hikariConfig.setJdbcUrl("jdbc:postgresql://10.100.1.13:5432/wms8d300?currentSchema=wms8prod");
//        hikariConfig.setUsername("wms8prod");
//        hikariConfig.setPassword("lmis9system");
//        hikariConfig.setAutoCommit(false);
//        hikariConfig.setMinimumIdle(1);
//        hikariConfig.setMaximumPoolSize(512);
//        Jdbc d100 = new Jdbc(hikariConfig);
//
//        hikariConfig = new HikariConfig();
//        hikariConfig.setDriverClassName("org.postgresql.Driver");
//        hikariConfig.setJdbcUrl("jdbc:postgresql://10.100.1.13:5432/wms8d900?currentSchema=wms8d900");
//        hikariConfig.setUsername("wms8prod");
//        hikariConfig.setPassword("lmis9system");
//        hikariConfig.setAutoCommit(true);
//        hikariConfig.setMinimumIdle(1);
//        hikariConfig.setMaximumPoolSize(512);
//        Jdbc d300 = new Jdbc(hikariConfig);
//
//        List<Map<String, Object>> d100_rows = d100.queryMany("select report_name, query_sql, summary_sql, report from dev_report");
//        for (Map<String, Object> row : d100_rows) {
//            Map<String, Object> fields = new HashMap<>(row);
//            fields.remove("report_name");
//            Map<String, Object> whereMap = new HashMap<>();
//            whereMap.put("report_name", row.get("report_name"));
//            d300.updateTable("dev_report", fields, whereMap);
//        }
//
//        d100_rows = d100.queryMany("select plan_code, plan_sql from sys_print_plan_inf");
//        for (Map<String, Object> row : d100_rows) {
//            Map<String, Object> fields = new HashMap<>(row);
//            fields.remove("plan_code");
//            Map<String, Object> whereMap = new HashMap<>();
//            whereMap.put("plan_code", row.get("plan_code"));
//            d300.updateTable("sys_print_plan_inf", fields, whereMap);
//        }
//        d100.close();
//        d300.close();
//    }
}
