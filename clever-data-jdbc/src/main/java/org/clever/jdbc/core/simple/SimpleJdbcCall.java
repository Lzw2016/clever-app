package org.clever.jdbc.core.simple;

import org.clever.jdbc.core.JdbcTemplate;
import org.clever.jdbc.core.RowMapper;
import org.clever.jdbc.core.SqlParameter;
import org.clever.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * SimpleJdbcCall 是一个多线程、可重用的对象，表示对存储过程或存储函数的调用。
 * 它提供元数据处理以简化访问基本存储过程功能所需的代码。您只需要提供过程函数的名称和执行调用时包含参数的 Map。
 * 提供的参数的名称将与创建存储过程时声明的输入和输出参数相匹配。
 *
 * <p>元数据处理基于 JDBC 驱动程序提供的 DatabaseMetaData。
 * 由于我们依赖于 JDBC 驱动程序，因此这种“自动检测”只能用于已知可提供准确元数据的数据库。
 * 目前包括 Derby、MySQL、Microsoft SQL Server、Oracle、DB2、Sybase 和 PostgreSQL。
 * 对于任何其他数据库，您需要显式声明所有参数。您当然可以显式声明所有参数，即使数据库提供了必要的元数据。
 * 在这种情况下，您声明的参数将优先。如果您想使用与存储过程编译期间声明的名称不匹配的参数名称，您也可以关闭任何元数据处理。
 *
 * <p>实际的插入是使用 Spring 的 {@link JdbcTemplate} 处理的
 *
 * <p>许多配置方法返回 SimpleJdbcCall 的当前实例，以便提供以“流畅”的界面风格将多个方法链接在一起的能力。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 16:53 <br/>
 *
 * @see java.sql.DatabaseMetaData
 * @see org.clever.jdbc.core.JdbcTemplate
 */
public class SimpleJdbcCall extends AbstractJdbcCall implements SimpleJdbcCallOperations {
    /**
     * 在创建基础 JdbcTemplate 时使用一个 JDBC 数据源参数的构造函数。
     *
     * @param dataSource {@code DataSource} 使用
     * @see org.clever.jdbc.core.JdbcTemplate#setDataSource
     */
    public SimpleJdbcCall(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 带有一个参数的替代构造函数，其中包含要使用的 JdbcTemplate。
     *
     * @param jdbcTemplate {@code JdbcTemplate} 使用
     * @see org.clever.jdbc.core.JdbcTemplate#setDataSource
     */
    public SimpleJdbcCall(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public SimpleJdbcCall withProcedureName(String procedureName) {
        setProcedureName(procedureName);
        setFunction(false);
        return this;
    }

    @Override
    public SimpleJdbcCall withFunctionName(String functionName) {
        setProcedureName(functionName);
        setFunction(true);
        return this;
    }

    @Override
    public SimpleJdbcCall withSchemaName(String schemaName) {
        setSchemaName(schemaName);
        return this;
    }

    @Override
    public SimpleJdbcCall withCatalogName(String catalogName) {
        setCatalogName(catalogName);
        return this;
    }

    @Override
    public SimpleJdbcCall withReturnValue() {
        setReturnValueRequired(true);
        return this;
    }

    @Override
    public SimpleJdbcCall declareParameters(SqlParameter... sqlParameters) {
        for (SqlParameter sqlParameter : sqlParameters) {
            if (sqlParameter != null) {
                addDeclaredParameter(sqlParameter);
            }
        }
        return this;
    }

    @Override
    public SimpleJdbcCall useInParameterNames(String... inParameterNames) {
        setInParameterNames(new LinkedHashSet<>(Arrays.asList(inParameterNames)));
        return this;
    }

    @Override
    public SimpleJdbcCall returningResultSet(String parameterName, RowMapper<?> rowMapper) {
        addDeclaredRowMapper(parameterName, rowMapper);
        return this;
    }

    @Override
    public SimpleJdbcCall withoutProcedureColumnMetaDataAccess() {
        setAccessCallParameterMetaData(false);
        return this;
    }

    @Override
    public SimpleJdbcCall withNamedBinding() {
        setNamedBinding(true);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeFunction(Class<T> returnType, Object... args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeFunction(Class<T> returnType, Map<String, ?> args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeFunction(Class<T> returnType, SqlParameterSource args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeObject(Class<T> returnType, Object... args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeObject(Class<T> returnType, Map<String, ?> args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeObject(Class<T> returnType, SqlParameterSource args) {
        return (T) doExecute(args).get(getScalarOutParameterName());
    }

    @Override
    public Map<String, Object> execute(Object... args) {
        return doExecute(args);
    }

    @Override
    public Map<String, Object> execute(Map<String, ?> args) {
        return doExecute(args);
    }

    @Override
    public Map<String, Object> execute(SqlParameterSource parameterSource) {
        return doExecute(parameterSource);
    }
}
