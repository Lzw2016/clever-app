package org.clever.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider} 接口的 DB2 特定实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:12 <br/>
 */
public class Db2CallMetaDataProvider extends GenericCallMetaDataProvider {
    public Db2CallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        super(databaseMetaData);
    }

    @Override
    public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
        try {
            setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
        } catch (SQLException ex) {
            logger.debug("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls' - " + ex.getMessage());
        }
        try {
            setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
        } catch (SQLException ex) {
            logger.debug("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls' - " + ex.getMessage());
        }
        try {
            setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
        } catch (SQLException ex) {
            logger.debug("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers' - " + ex.getMessage());
        }
        try {
            setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
        } catch (SQLException ex) {
            logger.debug("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers' - " + ex.getMessage());
        }
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
