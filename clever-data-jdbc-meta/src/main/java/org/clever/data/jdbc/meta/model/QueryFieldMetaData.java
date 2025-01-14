package org.clever.data.jdbc.meta.model;

import lombok.Data;

/**
 * sql查询的字段信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/14 15:09 <br/>
 */
@Data
public class QueryFieldMetaData {
    /**
     * 数据库名
     */
    private String databaseName;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 字段名
     */
    private String columnName;

//    tableAlias
//    columnAlias
}
