package org.clever.jdbc.core.simple;


import org.clever.jdbc.core.RowMapper;
import org.clever.jdbc.core.SqlParameter;
import org.clever.jdbc.core.namedparam.SqlParameterSource;

import java.util.Map;

/**
 * 为由 {@link SimpleJdbcCall} 实现的简单 JDBC 调用指定 API 的接口。
 * 这个接口通常不直接使用，但提供了增强可测试性的选项，因为它很容易被模拟或存根。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:00 <br/>
 */
public interface SimpleJdbcCallOperations {
    /**
     * 指定要使用的过程名称 - 这意味着我们将调用一个存储过程。
     *
     * @param procedureName 存储过程的名称
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withProcedureName(String procedureName);

    /**
     * 指定要使用的过程名称 - 这意味着我们将调用一个存储函数。
     *
     * @param functionName 存储函数的名称
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withFunctionName(String functionName);

    /**
     * 或者，指定包含存储过程的模式的名称。
     *
     * @param schemaName 模式的名称
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withSchemaName(String schemaName);

    /**
     * 或者，指定包含存储过程的目录的名称。
     * <p>为了与 Oracle DatabaseMetaData 保持一致，如果过程被声明为包的一部分，这将用于指定包名称。
     *
     * @param catalogName 目录或包名称
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withCatalogName(String catalogName);

    /**
     * 指示过程的返回值应包含在返回的结果中。
     *
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withReturnValue();

    /**
     * 如果需要，指定一个或多个参数。这些参数将补充有从数据库元数据中检索到的任何参数信息。
     * <p>请注意，只有声明为 {@code SqlParameter} 和 {@code SqlInOutParameter} 的参数将用于提供输入值。
     * 这与 {@code StoredProcedure} 类不同，后者出于向后兼容性原因，允许为声明为 {@code SqlOutParameter} 的参数提供输入值。
     *
     * @param sqlParameters 要使用的参数
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations declareParameters(SqlParameter... sqlParameters);

    /**
     * 还没有使用
     */
    SimpleJdbcCallOperations useInParameterNames(String... inParameterNames);

    /**
     * 用于指定存储过程何时返回 ResultSet 并且您希望它由 {@link RowMapper} 映射。
     * 将使用指定的参数名称返回结果。必须以正确的顺序声明多个 ResultSet。
     * <p>如果您使用的数据库是引用游标，则指定的名称必须与为数据库中的过程声明的参数名称相匹配。
     *
     * @param parameterName 返回结果的名称和/或引用游标参数的名称
     * @param rowMapper     将映射为每一行返回的数据的 RowMapper 实现
     */
    SimpleJdbcCallOperations returningResultSet(String parameterName, RowMapper<?> rowMapper);

    /**
     * 关闭对通过 JDBC 获取的参数元数据信息的任何处理。
     *
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withoutProcedureColumnMetaDataAccess();

    /**
     * 指示参数应按名称绑定。
     *
     * @return 这个 SimpleJdbcCall 的实例
     */
    SimpleJdbcCallOperations withNamedBinding();

    /**
     * 执行存储的函数并将获得的结果作为指定返回类型的对象返回。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的 in 参数值的可选数组。参数值的提供顺序必须与为存储过程定义的参数的顺序相同。
     */
    <T> T executeFunction(Class<T> returnType, Object... args);

    /**
     * 执行存储的函数并将获得的结果作为指定返回类型的对象返回。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的参数值的 Map
     */
    <T> T executeFunction(Class<T> returnType, Map<String, ?> args);

    /**
     * 执行存储的函数并将获得的结果作为指定返回类型的对象返回。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的参数值的 MapSqlParameterSource
     */
    <T> T executeFunction(Class<T> returnType, SqlParameterSource args);

    /**
     * 执行存储过程并将单出参数作为指定返回类型的对象返回。在有多个 out 参数的情况下，返回第一个并忽略其他 out 参数。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的 in 参数值的可选数组。参数值的提供顺序必须与为存储过程定义的参数的顺序相同。
     */
    <T> T executeObject(Class<T> returnType, Object... args);

    /**
     * 执行存储过程并将单出参数作为指定返回类型的对象返回。在有多个 out 参数的情况下，返回第一个并忽略其他 out 参数。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的参数值的 Map
     */
    <T> T executeObject(Class<T> returnType, Map<String, ?> args);

    /**
     * 执行存储过程并将单出参数作为指定返回类型的对象返回。在有多个 out 参数的情况下，返回第一个并忽略其他 out 参数。
     *
     * @param returnType 要返回的值的类型
     * @param args       包含要在调用中使用的参数值的 MapSqlParameterSource
     */
    <T> T executeObject(Class<T> returnType, SqlParameterSource args);

    /**
     * 执行存储过程并返回输出参数的映射，在参数声明中按名称键控。
     *
     * @param args 包含要在调用中使用的 in 参数值的可选数组。参数值的提供顺序必须与为存储过程定义的参数的顺序相同。
     * @return 输出参数的映射
     */
    Map<String, Object> execute(Object... args);

    /**
     * 执行存储过程并返回输出参数的映射，在参数声明中按名称键控。
     *
     * @param args 包含要在调用中使用的参数值的 Map
     * @return 输出参数的映射
     */
    Map<String, Object> execute(Map<String, ?> args);

    /**
     * 执行存储过程并返回输出参数的映射，在参数声明中按名称键控。
     *
     * @param args 包含要在调用中使用的参数值的 SqlParameterSource
     * @return 输出参数的映射
     */
    Map<String, Object> execute(SqlParameterSource args);
}
