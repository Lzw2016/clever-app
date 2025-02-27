package org.clever.data.jdbc.meta.utils;

import org.clever.core.Assert;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.*;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;

import java.util.Collection;
import java.util.List;

/**
 * 数据库元数据工具 <br/>
 * 1. 支持多个数据库 <br/>
 * 2. 获取 Schema 信息<br/>
 * 3. 获取 Table 信息<br/>
 * 4. 获取 Column 信息<br/>
 * 5. 获取 PrimaryKey 信息<br/>
 * 6. 获取 Index 信息<br/>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/28 22:58 <br/>
 */
public class MetaDataUtils {
    /**
     * 创建一个 DataBaseMetaData 对象
     */
    public static AbstractMetaData createMetaData(Jdbc jdbc) {
        Assert.notNull(jdbc, "参数 jdbc 不能为null");
        return switch (jdbc.getDbType()) {
            case MYSQL -> new MySQLMetaData(jdbc);
            case POSTGRE_SQL -> new PostgreSQLMetaData(jdbc);
            case ORACLE -> new OracleMetaData(jdbc);
            default -> throw new UnsupportedOperationException("不支持的数据库: " + jdbc.getDbType().getDb());
        };
    }

    /**
     * 获取数据库连接默认的Schema名
     */
    public static String currentSchema(Jdbc jdbc) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.currentSchema();
    }

    /**
     * 获取数据库元数据
     *
     * @param jdbc        数据源
     * @param schemasName 指定的 Schema 集合，不指定就获取所有的 Schema
     * @param tablesName  指定的 Table 集合，不指定就获取所有的 Table
     */
    public static List<Schema> getSchemas(Jdbc jdbc, Collection<String> schemasName, Collection<String> tablesName) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getSchemas(schemasName, tablesName);
    }

    /**
     * 获取当前库的所有 Schema
     */
    public static List<Schema> getSchemas(Jdbc jdbc) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getSchemas();
    }

    /**
     * 获取指定的 Schema
     */
    public static Schema getSchema(Jdbc jdbc, String schemaName) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getSchema(schemaName);
    }

    /**
     * 获取当前的 Schema
     */
    public static Schema getSchema(Jdbc jdbc) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getSchema();
    }

    /**
     * 获取指定的 Schema
     */
    public static Schema getSchema(Jdbc jdbc, String schemaName, Collection<String> tablesName) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getSchema(schemaName, tablesName);
    }

    /**
     * 获取指定的 Table
     */
    public static Table getTable(Jdbc jdbc, String schemaName, String tableName) {
        DataBaseMetaData metaData = createMetaData(jdbc);
        return metaData.getTable(schemaName, tableName);
    }
}
