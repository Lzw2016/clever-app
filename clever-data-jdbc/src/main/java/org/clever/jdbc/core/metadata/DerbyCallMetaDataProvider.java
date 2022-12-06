package org.clever.jdbc.core.metadata;


import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider} 接口的 Derby 特定实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:11 <br/>
 */
public class DerbyCallMetaDataProvider extends GenericCallMetaDataProvider {
    public DerbyCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        super(databaseMetaData);
    }

    @Override
    public String metaDataSchemaNameToUse(String schemaName) {
        if (schemaName != null) {
            return super.metaDataSchemaNameToUse(schemaName);
        }
        // Use current user schema if no schema specified...
        String userName = getUserName();
        return (userName != null ? userName.toUpperCase() : null);
    }
}
