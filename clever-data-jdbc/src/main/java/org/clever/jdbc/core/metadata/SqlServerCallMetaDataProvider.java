package org.clever.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider} 接口的 SQL Server 特定实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 22:02 <br/>
 */
public class SqlServerCallMetaDataProvider extends GenericCallMetaDataProvider {
    private static final String REMOVABLE_COLUMN_PREFIX = "@";
    private static final String RETURN_VALUE_NAME = "@RETURN_VALUE";

    public SqlServerCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        super(databaseMetaData);
    }

    @Override
    public String parameterNameToUse(String parameterName) {
        if (parameterName == null) {
            return null;
        } else if (parameterName.length() > 1 && parameterName.startsWith(REMOVABLE_COLUMN_PREFIX)) {
            return super.parameterNameToUse(parameterName.substring(1));
        } else {
            return super.parameterNameToUse(parameterName);
        }
    }

    @Override
    public boolean byPassReturnParameter(String parameterName) {
        return RETURN_VALUE_NAME.equals(parameterName);
    }
}
