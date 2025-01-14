package org.clever.data.jdbc.support;

import lombok.Data;

import java.sql.ResultSetMetaData;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/24 15:22 <br/>
 */
@Data
public class DbColumnMetaData {
    /** catalog {@link ResultSetMetaData#getCatalogName(int)} */
    private String catalogName;
    /** catalog {@link ResultSetMetaData#getSchemaName(int)} */
    private String schemaName;
    /** 优先取 {@link ResultSetMetaData#getSchemaName(int)} 不存在就取 {@link ResultSetMetaData#getCatalogName(int)} */
    private String databaseName;
    /** 表名 {@link ResultSetMetaData#getTableName(int)} */
    private String tableName;
    /** 字段名 {@link ResultSetMetaData#getColumnName(int)} */
    private String columnName;
    /** 字段显示名 {@link ResultSetMetaData#getColumnLabel(int)} */
    private String columnLabel;
    /** 字段类型名 {@link ResultSetMetaData#getColumnTypeName(int) } */
    private String columnTypeName;
    /** 字段类型(java.sql.Types) {@link ResultSetMetaData#getColumnType(int) */
    private Integer columnType;
}
