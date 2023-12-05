package org.clever.jdbc.core.simple;

import lombok.Getter;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.jdbc.core.*;
import org.clever.jdbc.core.metadata.CallMetaDataContext;
import org.clever.jdbc.core.namedparam.SqlParameterSource;
import org.clever.util.Assert;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;

/**
 * 抽象类为基于配置选项和数据库元数据的简单存储过程调用提供基本功能。
 * <p>此类为 {@link SimpleJdbcCall} 提供基础 SPI
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 16:57 <br/>
 */
public abstract class AbstractJdbcCall {
    /**
     * 子类可用的记录器。
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 用于执行 SQL 的低级类。
     */
    @Getter
    private final JdbcTemplate jdbcTemplate;
    /**
     * 用于检索和管理数据库元数据的上下文。
     */
    private final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();
    /**
     * SqlParameter 对象列表。
     */
    private final List<SqlParameter> declaredParameters = new ArrayList<>();
    /**
     * RefCursorResultSet RowMapper 对象列表。
     */
    private final Map<String, RowMapper<?>> declaredRowMappers = new LinkedHashMap<>();
    /**
     * 这个操作编译了吗？编译意味着至少检查是否提供了 DataSource 或 JdbcTemplate。
     */
    @Getter
    private volatile boolean compiled;
    /**
     * 生成的字符串用于调用语句。
     */
    @Getter
    protected String callString;
    /**
     * 一个委托，使我们能够根据此类声明的参数有效地创建 CallableStatementCreators
     */
    protected CallableStatementCreatorFactory callableStatementFactory;

    /**
     * 使用 {@link DataSource} 初始化时要使用的构造函数
     *
     * @param dataSource the DataSource to be used
     */
    protected AbstractJdbcCall(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 使用 {@link JdbcTemplate} 初始化时使用的构造函数
     *
     * @param jdbcTemplate 要使用的 JdbcTemplate
     */
    protected AbstractJdbcCall(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 设置存储过程的名称
     */
    public void setProcedureName(String procedureName) {
        this.callMetaDataContext.setProcedureName(procedureName);
    }

    /**
     * 获取存储过程的名称
     */
    public String getProcedureName() {
        return this.callMetaDataContext.getProcedureName();
    }

    /**
     * 设置要使用的 in 参数的名称
     */
    public void setInParameterNames(Set<String> inParameterNames) {
        this.callMetaDataContext.setLimitedInParameterNames(inParameterNames);
    }

    /**
     * 获取要使用的 in 参数的名称
     */
    public Set<String> getInParameterNames() {
        return this.callMetaDataContext.getLimitedInParameterNames();
    }

    /**
     * 设置要使用的目录名称
     */
    public void setCatalogName(String catalogName) {
        this.callMetaDataContext.setCatalogName(catalogName);
    }

    /**
     * 获取使用的目录名称
     */
    public String getCatalogName() {
        return this.callMetaDataContext.getCatalogName();
    }

    /**
     * 设置要使用的架构名称
     */
    public void setSchemaName(String schemaName) {
        this.callMetaDataContext.setSchemaName(schemaName);
    }

    /**
     * 获取使用的架构名称
     */
    public String getSchemaName() {
        return this.callMetaDataContext.getSchemaName();
    }

    /**
     * 指定此调用是否为函数调用。默认值为 {@code false}
     */
    public void setFunction(boolean function) {
        this.callMetaDataContext.setFunction(function);
    }

    /**
     * 这个调用是函数调用吗？
     */
    public boolean isFunction() {
        return this.callMetaDataContext.isFunction();
    }

    /**
     * 指定调用是否需要返回值。默认值为 {@code false}
     */
    public void setReturnValueRequired(boolean returnValueRequired) {
        this.callMetaDataContext.setReturnValueRequired(returnValueRequired);
    }

    /**
     * 调用是否需要返回值？
     */
    public boolean isReturnValueRequired() {
        return this.callMetaDataContext.isReturnValueRequired();
    }

    /**
     * 指定参数是否应按名称绑定。默认值为 {@code false}
     */
    public void setNamedBinding(boolean namedBinding) {
        this.callMetaDataContext.setNamedBinding(namedBinding);
    }

    /**
     * 参数应该按名称绑定吗？
     */
    public boolean isNamedBinding() {
        return this.callMetaDataContext.isNamedBinding();
    }

    /**
     * 指定是否应使用调用的参数元数据。默认值为 {@code true}
     */
    public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
        this.callMetaDataContext.setAccessCallParameterMetaData(accessCallParameterMetaData);
    }

    /**
     * 获取正在使用的 {@link CallableStatementCreatorFactory}
     */
    protected CallableStatementCreatorFactory getCallableStatementFactory() {
        Assert.state(this.callableStatementFactory != null, "No CallableStatementCreatorFactory available");
        return this.callableStatementFactory;
    }

    /**
     * 将声明的参数添加到调用的参数列表中。
     * <p>只有声明为 {@code SqlParameter} 和 {@code SqlInOutParameter} 的参数将用于提供输入值。
     * 这与 {@code StoredProcedure} 类不同，后者出于向后兼容性原因，允许为声明为 {@code SqlOutParameter} 的参数提供输入值。
     *
     * @param parameter 要添加的 {@link SqlParameter}
     */
    public void addDeclaredParameter(SqlParameter parameter) {
        Assert.notNull(parameter, "The supplied parameter must not be null");
        if (!StringUtils.hasText(parameter.getName())) {
            throw new InvalidDataAccessApiUsageException("You must specify a parameter name when declaring parameters for \"" + getProcedureName() + "\"");
        }
        this.declaredParameters.add(parameter);
        if (logger.isDebugEnabled()) {
            logger.debug("Added declared parameter for [" + getProcedureName() + "]: " + parameter.getName());
        }
    }

    /**
     * 为指定的参数或列添加 {@link org.clever.jdbc.core.RowMapper}
     *
     * @param parameterName 参数或列的名称
     * @param rowMapper     要使用的 RowMapper 实现
     */
    public void addDeclaredRowMapper(String parameterName, RowMapper<?> rowMapper) {
        this.declaredRowMappers.put(parameterName, rowMapper);
        if (logger.isDebugEnabled()) {
            logger.debug("Added row mapper for [" + getProcedureName() + "]: " + parameterName);
        }
    }

    //-------------------------------------------------------------------------
    // Methods handling compilation issues
    //-------------------------------------------------------------------------

    /**
     * 使用提供的参数和元数据以及其他设置编译此 JdbcCall。
     * <p>这将完成此对象的配置，随后的编译尝试将被忽略。这将在第一次执行未编译的调用时隐式调用。
     *
     * @throws org.clever.dao.InvalidDataAccessApiUsageException 如果对象没有被正确初始化，例如没有提供数据源
     */
    public final synchronized void compile() throws InvalidDataAccessApiUsageException {
        if (!isCompiled()) {
            if (getProcedureName() == null) {
                throw new InvalidDataAccessApiUsageException("Procedure or Function name is required");
            }
            try {
                this.jdbcTemplate.afterPropertiesSet();
            } catch (IllegalArgumentException ex) {
                throw new InvalidDataAccessApiUsageException(ex.getMessage());
            }
            compileInternal();
            this.compiled = true;
            if (logger.isDebugEnabled()) {
                logger.debug("SqlCall for " + (isFunction() ? "function" : "procedure") + " [" + getProcedureName() + "] compiled");
            }
        }
    }

    /**
     * 委托方法来执行实际的编译。
     * <p>子类可以覆盖这个模板方法来执行自己的编译。在此基类的编译完成后调用。
     */
    protected void compileInternal() {
        DataSource dataSource = getJdbcTemplate().getDataSource();
        Assert.state(dataSource != null, "No DataSource set");
        this.callMetaDataContext.initializeMetaData(dataSource);
        // Iterate over the declared RowMappers and register the corresponding SqlParameter
        this.declaredRowMappers.forEach((key, value) -> this.declaredParameters.add(this.callMetaDataContext.createReturnResultSetParameter(key, value)));
        this.callMetaDataContext.processParameters(this.declaredParameters);
        this.callString = this.callMetaDataContext.createCallString();
        if (logger.isDebugEnabled()) {
            logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
        }
        this.callableStatementFactory = new CallableStatementCreatorFactory(this.callString, this.callMetaDataContext.getCallParameters());
        onCompileInternal();
    }

    /**
     * 子类可以覆盖以对编译做出反应的挂钩方法。
     * 这个实现什么都不做。
     */
    protected void onCompileInternal() {
    }

    /**
     * 检查这个操作是否已经被编译过；如果尚未编译，请延迟编译它。
     * <p>由所有 {@code doExecute(...)} 方法自动调用。
     */
    protected void checkCompiled() {
        if (!isCompiled()) {
            logger.debug("JdbcCall call not compiled before execution - invoking compile");
            compile();
        }
    }

    //-------------------------------------------------------------------------
    // Methods handling execution
    //-------------------------------------------------------------------------

    /**
     * 使用传入的 {@link SqlParameterSource} 执行调用的委托方法
     *
     * @param parameterSource 调用中使用的参数名称和值
     * @return 输出参数的映射
     */
    protected Map<String, Object> doExecute(SqlParameterSource parameterSource) {
        checkCompiled();
        Map<String, Object> params = matchInParameterValuesWithCallParameters(parameterSource);
        return executeCallInternal(params);
    }

    /**
     * 使用传入的参数数组执行调用的委托方法。
     *
     * @param args 参数值数组。值的顺序必须与为存储过程声明的顺序相匹配。
     * @return 输出参数的映射
     */
    protected Map<String, Object> doExecute(Object... args) {
        checkCompiled();
        Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
        return executeCallInternal(params);
    }

    /**
     * 使用传入的参数映射执行调用的委托方法。
     *
     * @param args 参数名称和值的映射
     * @return 输出参数的映射
     */
    protected Map<String, Object> doExecute(Map<String, ?> args) {
        checkCompiled();
        Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
        return executeCallInternal(params);
    }

    /**
     * 委托方法执行实际的调用处理。
     */
    private Map<String, Object> executeCallInternal(Map<String, ?> args) {
        CallableStatementCreator csc = getCallableStatementFactory().newCallableStatementCreator(args);
        if (logger.isDebugEnabled()) {
            logger.debug("The following parameters are used for call " + getCallString() + " with " + args);
            int i = 1;
            for (SqlParameter param : getCallParameters()) {
                logger.debug(i + ": " + param.getName() +
                        ", SQL type " + param.getSqlType() +
                        ", type name " + param.getTypeName() +
                        ", parameter class [" + param.getClass().getName() + "]"
                );
                i++;
            }
        }
        return getJdbcTemplate().call(csc, getCallParameters());
    }

    /**
     * 获取单个输出参数或返回值的名称。
     * 用于带有一个输出参数的函数或过程。
     */
    protected String getScalarOutParameterName() {
        return this.callMetaDataContext.getScalarOutParameterName();
    }

    /**
     * 获取要用于调用的所有调用参数的列表。
     * 这包括基于元数据处理添加的任何参数。
     */
    protected List<SqlParameter> getCallParameters() {
        return this.callMetaDataContext.getCallParameters();
    }

    /**
     * 将提供的参数值与注册参数和通过元数据处理定义的参数相匹配。
     *
     * @param parameterSource 作为 {@link SqlParameterSource} 提供的参数值
     * @return 具有参数名称和值的 Map
     */
    protected Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
        return this.callMetaDataContext.matchInParameterValuesWithCallParameters(parameterSource);
    }

    /**
     * 将提供的参数值与注册参数和通过元数据处理定义的参数相匹配。
     *
     * @param args 作为数组提供的参数值
     * @return 具有参数名称和值的 Map
     */
    private Map<String, ?> matchInParameterValuesWithCallParameters(Object[] args) {
        return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
    }

    /**
     * 将提供的参数值与注册参数和通过元数据处理定义的参数相匹配。
     *
     * @param args Map 中提供的参数值
     * @return 具有参数名称和值的 Map
     */
    protected Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> args) {
        return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
    }
}
