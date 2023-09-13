package org.clever.data.jdbc.meta;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.zaxxer.hikari.HikariConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Column;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/09/13 15:55 <br/>
 */
@Slf4j
public class ZtfTest {
    public static HikariConfig postgresqlConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl("jdbc:postgresql://122.9.140.63:8000/wms85ztf");
        hikariConfig.setUsername("wms85");
        hikariConfig.setPassword("lmis9system");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(512);
        return hikariConfig;
    }

    public static Jdbc newPostgresql() {
        return new Jdbc(postgresqlConfig());
    }

    @Test
    public void t01() {
        Jdbc jdbc = newPostgresql();
        PostgreSQLMetaData metaData = new PostgreSQLMetaData(jdbc);
        metaData.addIgnoreTable("nc_evolutions");
        Schema schema = metaData.getSchema("public");
        List<DbField> list = new ArrayList<>();
        for (Table table : schema.getTables()) {
            StringBuilder sql = new StringBuilder("select * from ");
            sql.append(table.getName()).append(" where ");
            int idx = 0;
            for (Column column : table.getColumns()) {
                if (idx > 0) {
                    sql.append(" and ");
                }
                sql.append(column.getName()).append(" is not null ");
                idx++;
            }
            Map<String, Object> row = jdbc.queryFirst(sql.toString());
            long count = jdbc.queryCount("select * from " + table.getName());
            for (Column column : table.getColumns()) {
                DbField dbField = new DbField();
                dbField.setF5(table.getName());
                dbField.setF6(table.getComment());
                dbField.setF7(column.getName());
                dbField.setF8(column.getComment());
                if (row != null && row.containsKey(column.getName())) {
                    dbField.setF9(StringUtils.truncate(Conv.asString(row.get(column.getName())), 32766));
                } else {
                    String str = jdbc.queryFirstString("select " + column.getName() + " from " + table.getName() + " where " + column.getName() + " is not null ");
                    dbField.setF9(StringUtils.truncate(str, 32766));
                }
                dbField.setF10(count);
                list.add(dbField);
            }
        }
        log.info("--> {}", list.size());
        jdbc.close();
        String fileName = "C:\\Users\\lizw\\Downloads\\物流系统数据库数据清单.bak.xlsx";
        EasyExcel.write(fileName, DbField.class).sheet("数据分类分级清单（结构化数据）").doWrite(list);
    }
}

@Getter
@Setter
@EqualsAndHashCode
class DbField {
    @ExcelProperty("系统")
    private String f1 = "WMS系统";
    @ExcelProperty("主机IP")
    private String f2 = "192.168.104.44";
    @ExcelProperty("数据库名")
    private String f3 = "wms8";
    @ExcelProperty("数据库类型")
    private String f4 = "postgresql";
    @ExcelProperty("表名")
    private String f5 = "";
    @ExcelProperty("表注释")
    private String f6 = "";
    @ExcelProperty("字段名")
    private String f7 = "";
    @ExcelProperty("字段内容描述")
    private String f8 = "";
    @ExcelProperty("数据样本")
    private String f9 = "";
    @ExcelProperty("数据量")
    private Long f10 = 0L;
}
