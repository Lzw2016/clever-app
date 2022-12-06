package org.clever.jdbc.core.metadata;

import org.clever.jdbc.core.ColumnMapRowMapper;
import org.clever.jdbc.core.SqlOutParameter;
import org.clever.jdbc.core.SqlParameter;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * {@link CallMetaDataProvider} 接口的特定于 Postgres 的实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:10 <br/>
 */
public class PostgresCallMetaDataProvider extends GenericCallMetaDataProvider {
    private static final String RETURN_VALUE_NAME = "returnValue";

    public PostgresCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
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
        return Types.OTHER;
    }

    @Override
    public String metaDataSchemaNameToUse(String schemaName) {
        // Use public schema if no schema specified
        return (schemaName == null ? "public" : super.metaDataSchemaNameToUse(schemaName));
    }

    @Override
    public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
        if (meta.getSqlType() == Types.OTHER && "refcursor".equals(meta.getTypeName())) {
            return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
        } else {
            return super.createDefaultOutParameter(parameterName, meta);
        }
    }

    @Override
    public boolean byPassReturnParameter(String parameterName) {
        return RETURN_VALUE_NAME.equals(parameterName);
    }
}
