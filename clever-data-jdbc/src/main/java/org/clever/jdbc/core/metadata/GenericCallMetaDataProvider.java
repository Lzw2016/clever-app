package org.clever.jdbc.core.metadata;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.jdbc.core.SqlInOutParameter;
import org.clever.jdbc.core.SqlOutParameter;
import org.clever.jdbc.core.SqlParameter;
import org.clever.util.StringUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link CallMetaDataProvider} 接口的通用实现。
 * 可以扩展此类以提供特定于数据库的行为。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:07 <br/>
 */
public class GenericCallMetaDataProvider implements CallMetaDataProvider {
    /**
     * 子类可用的记录器。
     */
    protected static final Log logger = LogFactory.getLog(CallMetaDataProvider.class);
    private final String userName;
    private boolean supportsCatalogsInProcedureCalls = true;
    private boolean supportsSchemasInProcedureCalls = true;
    private boolean storesUpperCaseIdentifiers = true;
    private boolean storesLowerCaseIdentifiers = false;
    private boolean procedureColumnMetaDataUsed = false;
    private final List<CallParameterMetaData> callParameterMetaData = new ArrayList<>();

    /**
     * 用于使用提供的数据库元数据进行初始化的构造函数。
     *
     * @param databaseMetaData 要使用的元数据
     */
    protected GenericCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
        this.userName = databaseMetaData.getUserName();
    }

    @Override
    public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
        try {
            setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
        } catch (SQLException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls': " + ex.getMessage());
            }
        }
        try {
            setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
        } catch (SQLException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls': " + ex.getMessage());
            }
        }
        try {
            setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
        } catch (SQLException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers': " + ex.getMessage());
            }
        }
        try {
            setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
        } catch (SQLException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers': " + ex.getMessage());
            }
        }
    }

    @Override
    public void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData,
                                                      String catalogName,
                                                      String schemaName,
                                                      String procedureName) throws SQLException {
        this.procedureColumnMetaDataUsed = true;
        processProcedureColumns(databaseMetaData, catalogName, schemaName, procedureName);
    }

    @Override
    public List<CallParameterMetaData> getCallParameterMetaData() {
        return this.callParameterMetaData;
    }

    @Override
    public String procedureNameToUse(String procedureName) {
        if (procedureName == null) {
            return null;
        } else if (isStoresUpperCaseIdentifiers()) {
            return procedureName.toUpperCase();
        } else if (isStoresLowerCaseIdentifiers()) {
            return procedureName.toLowerCase();
        } else {
            return procedureName;
        }
    }

    @Override
    public String catalogNameToUse(String catalogName) {
        if (catalogName == null) {
            return null;
        } else if (isStoresUpperCaseIdentifiers()) {
            return catalogName.toUpperCase();
        } else if (isStoresLowerCaseIdentifiers()) {
            return catalogName.toLowerCase();
        } else {
            return catalogName;
        }
    }

    @Override
    public String schemaNameToUse(String schemaName) {
        if (schemaName == null) {
            return null;
        } else if (isStoresUpperCaseIdentifiers()) {
            return schemaName.toUpperCase();
        } else if (isStoresLowerCaseIdentifiers()) {
            return schemaName.toLowerCase();
        } else {
            return schemaName;
        }
    }

    @Override
    public String metaDataCatalogNameToUse(String catalogName) {
        if (isSupportsCatalogsInProcedureCalls()) {
            return catalogNameToUse(catalogName);
        } else {
            return null;
        }
    }

    @Override
    public String metaDataSchemaNameToUse(String schemaName) {
        if (isSupportsSchemasInProcedureCalls()) {
            return schemaNameToUse(schemaName);
        } else {
            return null;
        }
    }

    @Override
    public String parameterNameToUse(String parameterName) {
        if (parameterName == null) {
            return null;
        } else if (isStoresUpperCaseIdentifiers()) {
            return parameterName.toUpperCase();
        } else if (isStoresLowerCaseIdentifiers()) {
            return parameterName.toLowerCase();
        } else {
            return parameterName;
        }
    }

    @Override
    public boolean byPassReturnParameter(String parameterName) {
        return false;
    }

    @Override
    public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
        return new SqlOutParameter(parameterName, meta.getSqlType());
    }

    @Override
    public SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta) {
        return new SqlInOutParameter(parameterName, meta.getSqlType());
    }

    @Override
    public SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta) {
        return new SqlParameter(parameterName, meta.getSqlType());
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public boolean isReturnResultSetSupported() {
        return true;
    }

    @Override
    public boolean isRefCursorSupported() {
        return false;
    }

    @Override
    public int getRefCursorSqlType() {
        return Types.OTHER;
    }

    @Override
    public boolean isProcedureColumnMetaDataUsed() {
        return this.procedureColumnMetaDataUsed;
    }

    /**
     * 指定数据库是否支持在过程调用中使用目录名称。
     */
    protected void setSupportsCatalogsInProcedureCalls(boolean supportsCatalogsInProcedureCalls) {
        this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
    }

    /**
     * 数据库是否支持在过程调用中使用目录名称？
     */
    @Override
    public boolean isSupportsCatalogsInProcedureCalls() {
        return this.supportsCatalogsInProcedureCalls;
    }

    /**
     * 指定数据库是否支持在过程调用中使用模式名称。
     */
    protected void setSupportsSchemasInProcedureCalls(boolean supportsSchemasInProcedureCalls) {
        this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
    }

    /**
     * 数据库是否支持在过程调用中使用模式名称？
     */
    @Override
    public boolean isSupportsSchemasInProcedureCalls() {
        return this.supportsSchemasInProcedureCalls;
    }

    /**
     * 指定数据库是否使用大写字母作为标识符。
     */
    protected void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
        this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
    }

    /**
     * 数据库是否使用大写字母作为标识符？
     */
    protected boolean isStoresUpperCaseIdentifiers() {
        return this.storesUpperCaseIdentifiers;
    }

    /**
     * 指定数据库是否使用小写字母作为标识符。
     */
    protected void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
        this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
    }

    /**
     * 数据库是否使用小写字母作为标识符？
     */
    protected boolean isStoresLowerCaseIdentifiers() {
        return this.storesLowerCaseIdentifiers;
    }

    /**
     * 处理过程列元数据。
     */
    private void processProcedureColumns(DatabaseMetaData databaseMetaData, String catalogName, String schemaName, String procedureName) {
        String metaDataCatalogName = metaDataCatalogNameToUse(catalogName);
        String metaDataSchemaName = metaDataSchemaNameToUse(schemaName);
        String metaDataProcedureName = procedureNameToUse(procedureName);
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving meta-data for " + metaDataCatalogName + '/' + metaDataSchemaName + '/' + metaDataProcedureName);
        }
        try {
            List<String> found = new ArrayList<>();
            boolean function = false;
            try (ResultSet procedures = databaseMetaData.getProcedures(
                    metaDataCatalogName, metaDataSchemaName, metaDataProcedureName)) {
                while (procedures.next()) {
                    found.add(procedures.getString("PROCEDURE_CAT") +
                            '.' + procedures.getString("PROCEDURE_SCHEM") +
                            '.' + procedures.getString("PROCEDURE_NAME")
                    );
                }
            }
            if (found.isEmpty()) {
                // Functions not exposed as procedures anymore on PostgreSQL driver 42.2.11
                try (ResultSet functions = databaseMetaData.getFunctions(
                        metaDataCatalogName, metaDataSchemaName, metaDataProcedureName)) {
                    while (functions.next()) {
                        found.add(functions.getString("FUNCTION_CAT") +
                                '.' + functions.getString("FUNCTION_SCHEM") +
                                '.' + functions.getString("FUNCTION_NAME")
                        );
                        function = true;
                    }
                }
            }
            if (found.size() > 1) {
                throw new InvalidDataAccessApiUsageException("Unable to determine the correct call signature - multiple signatures for '" +
                        metaDataProcedureName + "': found " + found + " " + (function ? "functions" : "procedures")
                );
            } else if (found.isEmpty()) {
                if (metaDataProcedureName != null && metaDataProcedureName.contains(".") &&
                        !StringUtils.hasText(metaDataCatalogName)) {
                    String packageName = metaDataProcedureName.substring(0, metaDataProcedureName.indexOf('.'));
                    throw new InvalidDataAccessApiUsageException("Unable to determine the correct call signature for '" + metaDataProcedureName +
                            "' - package name should be specified separately using '.withCatalogName(\"" + packageName + "\")'"
                    );
                } else if ("Oracle".equals(databaseMetaData.getDatabaseProductName())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Oracle JDBC driver did not return procedure/function/signature for '" +
                                metaDataProcedureName + "' - assuming a non-exposed synonym"
                        );
                    }
                } else {
                    throw new InvalidDataAccessApiUsageException("Unable to determine the correct call signature - no " +
                            "procedure/function/signature for '" + metaDataProcedureName + "'"
                    );
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieving column meta-data for " + (function ? "function" : "procedure") + ' ' +
                        metaDataCatalogName + '/' + metaDataSchemaName + '/' + metaDataProcedureName
                );
            }
            try (ResultSet columns = function ?
                    databaseMetaData.getFunctionColumns(metaDataCatalogName, metaDataSchemaName, metaDataProcedureName, null) :
                    databaseMetaData.getProcedureColumns(metaDataCatalogName, metaDataSchemaName, metaDataProcedureName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    int columnType = columns.getInt("COLUMN_TYPE");
                    if (columnName == null && isInOrOutColumn(columnType, function)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Skipping meta-data for: " + columnType + " " + columns.getInt("DATA_TYPE") +
                                    " " + columns.getString("TYPE_NAME") + " " + columns.getInt("NULLABLE") +
                                    " (probably a member of a collection)"
                            );
                        }
                    } else {
                        int nullable = (function ? DatabaseMetaData.functionNullable : DatabaseMetaData.procedureNullable);
                        CallParameterMetaData meta = new CallParameterMetaData(
                                function, columnName, columnType,
                                columns.getInt("DATA_TYPE"),
                                columns.getString("TYPE_NAME"),
                                columns.getInt("NULLABLE") == nullable
                        );
                        this.callParameterMetaData.add(meta);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Retrieved meta-data: " + meta.getParameterName() + " " +
                                    meta.getParameterType() + " " + meta.getSqlType() + " " +
                                    meta.getTypeName() + " " + meta.isNullable()
                            );
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Error while retrieving meta-data for procedure columns. " +
                                "Consider declaring explicit parameters -- for example, via SimpleJdbcCall#addDeclaredParameter().", ex
                );
            }
            // Although we could invoke `this.callParameterMetaData.clear()` so that
            // we don't retain a partial list of column names (like we do in
            // GenericTableMetaDataProvider.processTableColumns(...)), we choose
            // not to do that here, since invocation of the stored procedure will
            // likely fail anyway with an incorrect argument list.
        }
    }

    private static boolean isInOrOutColumn(int columnType, boolean function) {
        if (function) {
            return (columnType == DatabaseMetaData.functionColumnIn
                    || columnType == DatabaseMetaData.functionColumnInOut
                    || columnType == DatabaseMetaData.functionColumnOut);
        } else {
            return (columnType == DatabaseMetaData.procedureColumnIn
                    || columnType == DatabaseMetaData.procedureColumnInOut
                    || columnType == DatabaseMetaData.procedureColumnOut);
        }
    }
}
