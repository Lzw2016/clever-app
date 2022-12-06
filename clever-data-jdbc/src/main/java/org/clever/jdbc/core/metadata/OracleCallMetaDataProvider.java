package org.clever.jdbc.core.metadata;

import org.clever.jdbc.core.ColumnMapRowMapper;
import org.clever.jdbc.core.SqlOutParameter;
import org.clever.jdbc.core.SqlParameter;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * {@link CallMetaDataProvider} 接口的特定于 Oracle 的实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:10 <br/>
 */
public class OracleCallMetaDataProvider extends GenericCallMetaDataProvider {
    private static final String REF_CURSOR_NAME = "REF CURSOR";

    public OracleCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        super(databaseMetaData);
    }

    @Override
    public boolean isReturnResultSetSupported() {
        return false;
    }

    @Override
    public boolean isRefCursorSupported() {
        return true;
    }

    @Override
    public int getRefCursorSqlType() {
        return -10;
    }

    @Override

    public String metaDataCatalogNameToUse(String catalogName) {
        // Oracle uses catalog name for package name or an empty string if no package
        return (catalogName == null ? "" : catalogNameToUse(catalogName));
    }

    @Override
    public String metaDataSchemaNameToUse(String schemaName) {
        // Use current user schema if no schema specified
        return (schemaName == null ? getUserName() : super.metaDataSchemaNameToUse(schemaName));
    }

    @Override
    public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
        if (meta.getSqlType() == Types.OTHER && REF_CURSOR_NAME.equals(meta.getTypeName())) {
            return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
        } else {
            return super.createDefaultOutParameter(parameterName, meta);
        }
    }
}
