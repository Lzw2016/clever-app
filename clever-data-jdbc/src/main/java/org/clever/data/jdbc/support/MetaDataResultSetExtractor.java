package org.clever.data.jdbc.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.reflection.ReflectionsUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/24 15:20 <br/>
 */
@Slf4j
public class MetaDataResultSetExtractor implements ResultSetExtractor<List<DbColumnMetaData>> {
    public static MetaDataResultSetExtractor create(String sql) {
        return new MetaDataResultSetExtractor(sql, RenameStrategy.None);
    }

    public static MetaDataResultSetExtractor create(String sql, RenameStrategy renameStrategy) {
        return new MetaDataResultSetExtractor(sql, renameStrategy);
    }

    /**
     * 原始的SQL语句
     */
    public final String sql;
    /**
     * 是否需要重命名
     */
    public final boolean needRename;
    /**
     * 字段名重命名策略
     */
    private final RenameStrategy renameStrategy;
    /**
     * 表头元数据
     */
    public final List<DbColumnMetaData> headMeta;

    public MetaDataResultSetExtractor(String sql, RenameStrategy renameStrategy) {
        this.sql = sql;
        if (renameStrategy == null) {
            renameStrategy = RenameStrategy.None;
        }
        this.needRename = !RenameStrategy.None.equals(renameStrategy);
        this.renameStrategy = renameStrategy;
        this.headMeta = new ArrayList<>(32);
    }

    @Override
    public List<DbColumnMetaData> extractData(ResultSet rs) throws SQLException, DataAccessException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int idx = 1; idx <= columnCount; idx++) {
            String catalogName = metaData.getCatalogName(idx);
            String schemaName = metaData.getSchemaName(idx);
            String tableName = metaData.getTableName(idx);
            String columnName = metaData.getColumnName(idx);
            String columnLabel = metaData.getColumnLabel(idx);
            String columnTypeName = metaData.getColumnTypeName(idx);
            Integer columnType = metaData.getColumnType(idx);
            // 特定数据库处理
            if (metaData instanceof org.postgresql.jdbc.PgResultSetMetaData) {
                // Postgres
                org.postgresql.core.Field[] fields = ReflectionsUtils.getFieldValue(metaData, "fields", false);
                if (fields != null && fields.length > idx) {
                    org.postgresql.core.Field field = fields[idx - 1];
                    if (field != null && field.getMetadata() != null) {
                        org.postgresql.jdbc.FieldMetadata fieldMetadata = field.getMetadata();
                        String tmp = ReflectionsUtils.getFieldValue(fieldMetadata, "columnName", false);
                        if (StringUtils.isNotBlank(tmp)) {
                            columnName = tmp;
                        }
                        tmp = ReflectionsUtils.getFieldValue(fieldMetadata, "tableName", false);
                        if (StringUtils.isNotBlank(tmp)) {
                            tableName = tmp;
                        }
                        tmp = ReflectionsUtils.getFieldValue(fieldMetadata, "schemaName", false);
                        if (StringUtils.isNotBlank(tmp)) {
                            schemaName = tmp;
                        }
                        tmp = field.getColumnLabel();
                        if (StringUtils.isNotBlank(tmp)) {
                            columnLabel = tmp;
                        }
                    }
                }
            } else if (metaData instanceof com.mysql.cj.jdbc.result.ResultSetMetaData) {
                // MySQL
                com.mysql.cj.result.Field[] fields = ReflectionsUtils.getFieldValue(metaData, "fields", false);
                if (fields != null && fields.length > idx) {
                    com.mysql.cj.result.Field field = fields[idx - 1];
                    if (field != null) {
                        com.mysql.cj.util.LazyString tmp = ReflectionsUtils.getFieldValue(field, "originalColumnName", false);
                        if (tmp != null && StringUtils.isNotBlank(tmp.toString())) {
                            columnName = tmp.toString();
                        }
                        tmp = ReflectionsUtils.getFieldValue(field, "originalTableName", false);
                        if (tmp != null && StringUtils.isNotBlank(tmp.toString())) {
                            tableName = tmp.toString();
                        }
                        tmp = ReflectionsUtils.getFieldValue(field, "databaseName", false);
                        if (tmp != null && StringUtils.isNotBlank(tmp.toString())) {
                            catalogName = tmp.toString();
                        }
                        String tmpStr = field.getColumnLabel();
                        if (StringUtils.isNotBlank(tmpStr)) {
                            columnLabel = tmpStr;
                        }
                    }
                }
            }
            // 后置处理
            if (StringUtils.isBlank(columnLabel)) {
                columnLabel = columnName;
            }
            if (needRename) {
                columnLabel = NamingUtils.rename(columnLabel, renameStrategy);
            }
            // 构建 DbColumnMeta
            DbColumnMetaData columnMetaData = new DbColumnMetaData();
            columnMetaData.setCatalogName(catalogName);
            columnMetaData.setSchemaName(schemaName);
            if (StringUtils.isNotBlank(schemaName)) {
                columnMetaData.setDatabaseName(schemaName);
            } else {
                columnMetaData.setDatabaseName(catalogName);
            }
            columnMetaData.setTableName(tableName);
            columnMetaData.setColumnName(columnName);
            columnMetaData.setColumnLabel(columnLabel);
            columnMetaData.setColumnTypeName(columnTypeName);
            columnMetaData.setColumnType(columnType);
            headMeta.add(columnMetaData);
        }
        return headMeta;
    }
}
