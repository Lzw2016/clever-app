package org.clever.jdbc.core.metadata;

import org.clever.jdbc.core.SqlParameter;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * 指定由提供调用元数据的类实现的 API 的接口。
 * <p>这是供 Spring 的 {@link org.clever.jdbc.core.simple.SimpleJdbcCall} 内部使用的。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:04 <br/>
 */
public interface CallMetaDataProvider {
    /**
     * 使用提供的 DatabaseMetData 进行初始化。
     *
     * @param databaseMetaData 用于检索数据库特定信息
     * @throws SQLException 在初始化失败的情况下
     */
    void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

    /**
     * 初始化程序列元数据的数据库特定管理。仅对受支持的数据库调用此方法。可以通过指定不应使用列元数据来关闭此初始化。
     *
     * @param databaseMetaData 用于检索数据库特定信息
     * @param catalogName      要使用的目录名称（如果没有，则为 {@code null}）
     * @param schemaName       要使用的模式名称的名称（如果没有，则为 {@code null}）
     * @param procedureName    存储过程的名称
     * @throws SQLException 在初始化失败的情况下
     * @see org.clever.jdbc.core.simple.SimpleJdbcCall#withoutProcedureColumnMetaDataAccess()
     */
    void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData,
                                               String catalogName,
                                               String schemaName,
                                               String procedureName) throws SQLException;

    /**
     * 提供对传入的过程名称的任何修改以匹配当前使用的元数据。这可能包括改变案例。
     */
    String procedureNameToUse(String procedureName);

    /**
     * 提供对传入的目录名称的任何修改以匹配当前使用的元数据。这可能包括更改大小写。
     */
    String catalogNameToUse(String catalogName);

    /**
     * 提供传入的模式名称的任何修改以匹配当前使用的元数据。这可能包括改变案例。
     */
    String schemaNameToUse(String schemaName);

    /**
     * 提供对传入的目录名称的任何修改以匹配当前使用的元数据。返回值将用于元数据查找。这可能包括更改使用的案例或提供基本目录（如果未提供）。
     */
    String metaDataCatalogNameToUse(String catalogName);

    /**
     * 提供传入的模式名称的任何修改以匹配当前使用的元数据。返回值将用于元数据查找。这可能包括更改使用的案例或提供基本模式（如果未提供）。
     */
    String metaDataSchemaNameToUse(String schemaName);

    /**
     * 提供对传入的列名的任何修改以匹配当前使用的元数据。这可能包括改变案例。
     *
     * @param parameterName 列的参数名称
     */
    String parameterNameToUse(String parameterName);

    /**
     * 根据提供的元数据创建默认输出参数。这在没有进行显式参数声明时使用。
     *
     * @param parameterName 参数名称
     * @param meta          用于此调用的元数据
     * @return the configured SqlOutParameter
     */
    SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta);

    /**
     * 根据提供的元数据创建一个默认的 inout 参数。这在没有进行显式参数声明时使用。
     *
     * @param parameterName 参数名称
     * @param meta          用于此调用的元数据
     * @return 配置的 SqlInOutParameter
     */
    SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta);

    /**
     * 根据提供的元数据创建默认参数。这在没有进行显式参数声明时使用。
     *
     * @param parameterName 参数名称
     * @param meta          用于此调用的元数据
     * @return 配置的 SqlParameter
     */
    SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta);

    /**
     * 获取当前用户的名称。用于元数据查找等。
     *
     * @return 来自数据库连接的当前用户名
     */
    String getUserName();

    /**
     * 此数据库是否支持返回应使用 JDBC 调用检索的结果集：{@link java.sql.Statement#getResultSet()}?
     */
    boolean isReturnResultSetSupported();

    /**
     * 此数据库是否支持将 ResultSet 作为引用游标返回，以便使用 {@link java.sql.CallableStatement#getObject(int)} 检索指定列。
     */
    boolean isRefCursorSupported();

    /**
     * 如果支持此功能，则获取返回结果集作为引用游标的列的 {@link java.sql.Types} 类型。
     */
    int getRefCursorSqlType();

    /**
     * 我们是否将元数据用于过程列？
     */
    boolean isProcedureColumnMetaDataUsed();

    /**
     * 我们是否应该绕过具有指定名称的返回参数。这允许数据库特定实现跳过对数据库调用返回的特定结果的处理。
     */
    boolean byPassReturnParameter(String parameterName);

    /**
     * 获取当前使用的调用参数元数据。
     *
     * @return {@link CallParameterMetaData} 列表
     */
    List<CallParameterMetaData> getCallParameterMetaData();

    /**
     * 数据库是否支持在过程调用中使用目录名称？
     */
    boolean isSupportsCatalogsInProcedureCalls();

    /**
     * 数据库是否支持在过程调用中使用模式名称？
     */
    boolean isSupportsSchemasInProcedureCalls();
}
