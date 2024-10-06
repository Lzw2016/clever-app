package org.clever.data.jdbc.support;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/09 16:29 <br/>
 */
@Slf4j
public class MapRowMapper extends ColumnMapRowMapper {
    public static MapRowMapper create() {
        return new MapRowMapper(RenameStrategy.None);
    }

    public static MapRowMapper create(RenameStrategy renameStrategy) {
        return new MapRowMapper(renameStrategy);
    }

    /**
     * 是否需要重命名
     */
    public final boolean needRename;
    /**
     * 字段名重命名策略
     */
    private final RenameStrategy renameStrategy;
    /**
     * 重命名缓存
     */
    public final Map<String, String> renameCache;

    public MapRowMapper(RenameStrategy renameStrategy) {
        if (renameStrategy == null) {
            renameStrategy = RenameStrategy.None;
        }
        this.needRename = !RenameStrategy.None.equals(renameStrategy);
        this.renameStrategy = renameStrategy;
        if (needRename) {
            renameCache = new HashMap<>(32);
        } else {
            renameCache = Collections.emptyMap();
        }
    }

    @NotNull
    @Override
    protected String getColumnKey(@NotNull String columnName) {
        // 字段重命名
        if (needRename) {
            columnName = renameCache.computeIfAbsent(columnName, name -> NamingUtils.rename(name, renameStrategy));
        }
        return columnName;
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        // 参考 JdbcUtils.getResultSetValue(rs, index) 部分
        Object obj = rs.getObject(index);
        if (obj == null) {
            return null;
        }
        String className = obj.getClass().getName();
        if (obj instanceof Blob blob) { // ------------------------------------------------------------------------------------------------- byte[]
            obj = blob.getBytes(1, (int) blob.length());
        } else if (obj instanceof Clob clob) { // ------------------------------------------------------------------------------------------ String
            obj = clob.getSubString(1, (int) clob.length());
        } else if ("oracle.sql.TIMESTAMP".equals(className)
            || "oracle.sql.TIMESTAMPTZ".equals(className)
            || "oracle.sql.TIMESTAMPLTZ".equals(className)) { // ------------------------------------------------------------------ java.sql.Timestamp
            obj = rs.getTimestamp(index);
        } else if (className.startsWith("oracle.sql.DATE")) {
            String metaDataClassName = rs.getMetaData().getColumnClassName(index);
            if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) { // -------------- java.sql.Timestamp
                obj = rs.getTimestamp(index);
            } else { // --------------------------------------------------------------------------------------------------------------- java.util.Date
                java.sql.Date date = rs.getDate(index);
                obj = date == null ? null : new java.util.Date(date.getTime());
            }
        } else if (obj instanceof java.sql.Date) { // --------------------------------------------------------------------------------- java.util.Date
            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
                obj = rs.getTimestamp(index);
            }
        } else {
            if (obj instanceof Integer
                || obj instanceof Long
                || obj instanceof Double
                || obj instanceof String
                || obj instanceof Boolean
                || obj instanceof BigDecimal
                || obj instanceof java.util.Date
                || obj instanceof byte[]
                || obj instanceof Byte[]) {
                return obj;
            }
            // 自定义处理
            obj = JdbcTypeMappingUtils.getColumnType(rs, index, obj);
        }
        return obj;
    }
}
