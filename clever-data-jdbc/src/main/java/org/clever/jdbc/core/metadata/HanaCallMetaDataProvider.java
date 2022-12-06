package org.clever.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider} 接口的 SAP HANA 特定实现。
 * 此类旨在供简单 JDBC 类内部使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:12 <br/>
 */
public class HanaCallMetaDataProvider extends GenericCallMetaDataProvider {
    public HanaCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        super(databaseMetaData);
    }

    @Override
    public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
        super.initializeWithMetaData(databaseMetaData);
        setStoresUpperCaseIdentifiers(false);
    }
}
