package org.clever.data.jdbc.support;

import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.dao.DataAccessException;
import org.clever.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/24 15:20 <br/>
 */
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
        for (int i = 1; i <= columnCount; i++) {
            DbColumnMetaData columnMetaData = new DbColumnMetaData();
            columnMetaData.setTableName(metaData.getTableName(i));
            String columnName = metaData.getColumnName(i);
            if (needRename) {
                columnName = NamingUtils.rename(columnName, renameStrategy);
            }
            columnMetaData.setColumnName(columnName);
            columnMetaData.setColumnTypeName(metaData.getColumnTypeName(i));
            columnMetaData.setColumnType(metaData.getColumnType(i));
            headMeta.add(columnMetaData);
        }
        return headMeta;
    }
}
