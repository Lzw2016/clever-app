package org.clever.jdbc.core.metadata;

import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.jdbc.core.*;
import org.clever.jdbc.core.namedparam.SqlParameterSource;
import org.clever.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.util.Assert;
import org.clever.util.CollectionUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * 用于管理用于配置和执行存储过程调用的上下文元数据的类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 16:58 <br/>
 */
public class CallMetaDataContext {
    // 子类可用的记录器
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    // 要调用的过程的名称
    private String procedureName;
    // 调用目录名称
    private String catalogName;
    // 调用模式的名称
    private String schemaName;
    // 在调用执行中使用的 SqlParameter 对象列表
    private List<SqlParameter> callParameters = new ArrayList<>();
    // 用于输出映射中返回值的实际名称
    private String actualFunctionReturnName;
    // 一组参数名称以排除任何未列出的使用
    private Set<String> limitedInParameterNames = new HashSet<>();
    // out 参数的 SqlParameter 名称列表
    private List<String> outParameterNames = new ArrayList<>();
    // 指示这是过程还是函数
    private boolean function = false;
    // 指示是否应包括此过程的返回值
    private boolean returnValueRequired = false;
    // 我们是否应该访问调用参数元数据信息
    private boolean accessCallParameterMetaData = true;
    // 我们应该按名称绑定参数吗
    private boolean namedBinding;
    // 呼叫元数据的提供者
    private CallMetaDataProvider metaDataProvider;

    /**
     * 指定用于函数返回值的名称
     */
    public void setFunctionReturnName(String functionReturnName) {
        this.actualFunctionReturnName = functionReturnName;
    }

    /**
     * 获取用于函数返回值的名称
     */
    public String getFunctionReturnName() {
        return (this.actualFunctionReturnName != null ? this.actualFunctionReturnName : "return");
    }

    /**
     * 指定要使用的一组有限的 in 参数
     */
    public void setLimitedInParameterNames(Set<String> limitedInParameterNames) {
        this.limitedInParameterNames = limitedInParameterNames;
    }

    /**
     * 获取要使用的一组有限的 in 参数
     */
    public Set<String> getLimitedInParameterNames() {
        return this.limitedInParameterNames;
    }

    /**
     * 指定输出参数的名称
     */
    public void setOutParameterNames(List<String> outParameterNames) {
        this.outParameterNames = outParameterNames;
    }

    /**
     * 获取输出参数名称的列表
     */
    public List<String> getOutParameterNames() {
        return this.outParameterNames;
    }

    /**
     * 指定过程的名称
     */
    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    /**
     * 获取过程的名称
     */
    public String getProcedureName() {
        return this.procedureName;
    }

    /**
     * 指定目录的名称
     */
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    /**
     * 获取目录的名称
     */
    public String getCatalogName() {
        return this.catalogName;
    }

    /**
     * 指定架构的名称
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * 获取模式的名称
     */
    public String getSchemaName() {
        return this.schemaName;
    }

    /**
     * 指定此调用是否为函数调用
     */
    public void setFunction(boolean function) {
        this.function = function;
    }

    /**
     * 检查此调用是否为函数调用
     */
    public boolean isFunction() {
        return this.function;
    }

    /**
     * 指定是否需要返回值
     */
    public void setReturnValueRequired(boolean returnValueRequired) {
        this.returnValueRequired = returnValueRequired;
    }

    /**
     * 检查是否需要返回值
     */
    public boolean isReturnValueRequired() {
        return this.returnValueRequired;
    }

    /**
     * 指定是否应访问调用参数元数据
     */
    public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
        this.accessCallParameterMetaData = accessCallParameterMetaData;
    }

    /**
     * 检查是否应访问调用参数元数据
     */
    public boolean isAccessCallParameterMetaData() {
        return this.accessCallParameterMetaData;
    }

    /**
     * 指定参数是否应按名称绑定
     */
    public void setNamedBinding(boolean namedBinding) {
        this.namedBinding = namedBinding;
    }

    /**
     * 检查参数是否应按名称绑定
     */
    public boolean isNamedBinding() {
        return this.namedBinding;
    }

    /**
     * 使用数据库中的元数据初始化此类
     *
     * @param dataSource 用于检索元数据的数据源
     */
    public void initializeMetaData(DataSource dataSource) {
        this.metaDataProvider = CallMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
    }

    private CallMetaDataProvider obtainMetaDataProvider() {
        Assert.state(this.metaDataProvider != null, "No CallMetaDataProvider - call initializeMetaData first");
        return this.metaDataProvider;
    }

    /**
     * 根据用于所用数据库的 JDBC 驱动程序提供的支持，创建 ReturnResultSetParameterSqlOutParameter
     *
     * @param parameterName 参数的名称（也用作输出中返回的列表的名称）
     * @param rowMapper     用于映射结果集中返回的数据的 RowMapper 实现
     * @return 适当的 SqlParameter
     */
    public SqlParameter createReturnResultSetParameter(String parameterName, RowMapper<?> rowMapper) {
        CallMetaDataProvider provider = obtainMetaDataProvider();
        if (provider.isReturnResultSetSupported()) {
            return new SqlReturnResultSet(parameterName, rowMapper);
        } else {
            if (provider.isRefCursorSupported()) {
                return new SqlOutParameter(parameterName, provider.getRefCursorSqlType(), rowMapper);
            } else {
                throw new InvalidDataAccessApiUsageException("Return of a ResultSet from a stored procedure is not supported");
            }
        }
    }

    /**
     * 获取此调用的单个输出参数的名称。
     * 如果有多个参数，则返回第一个参数的名称。
     */
    public String getScalarOutParameterName() {
        if (isFunction()) {
            return getFunctionReturnName();
        } else {
            if (this.outParameterNames.size() > 1) {
                logger.info("Accessing single output value when procedure has more than one output parameter");
            }
            return (!this.outParameterNames.isEmpty() ? this.outParameterNames.get(0) : null);
        }
    }

    /**
     * 获取要在调用执行中使用的 SqlParameter 对象列表
     */
    public List<SqlParameter> getCallParameters() {
        return this.callParameters;
    }

    /**
     * 处理提供的参数列表，如果使用过程列元数据，参数将与元数据信息匹配，任何缺失的参数将自动包含在内。
     *
     * @param parameters 用作基础的参数列表
     */
    public void processParameters(List<SqlParameter> parameters) {
        this.callParameters = reconcileParameters(parameters);
    }

    /**
     * 将提供的参数与可用的元数据进行协调，并在适当的地方添加新的参数。
     */
    protected List<SqlParameter> reconcileParameters(List<SqlParameter> parameters) {
        CallMetaDataProvider provider = obtainMetaDataProvider();
        final List<SqlParameter> declaredReturnParams = new ArrayList<>();
        final Map<String, SqlParameter> declaredParams = new LinkedHashMap<>();
        boolean returnDeclared = false;
        List<String> outParamNames = new ArrayList<>();
        List<String> metaDataParamNames = new ArrayList<>();
        // Get the names of the meta-data parameters
        for (CallParameterMetaData meta : provider.getCallParameterMetaData()) {
            if (!meta.isReturnParameter()) {
                metaDataParamNames.add(lowerCase(meta.getParameterName()));
            }
        }
        // Separate implicit return parameters from explicit parameters...
        for (SqlParameter param : parameters) {
            if (param.isResultsParameter()) {
                declaredReturnParams.add(param);
            } else {
                String paramName = param.getName();
                if (paramName == null) {
                    throw new IllegalArgumentException("Anonymous parameters not supported for calls - " +
                            "please specify a name for the parameter of SQL type " + param.getSqlType()
                    );
                }
                String paramNameToMatch = lowerCase(provider.parameterNameToUse(paramName));
                declaredParams.put(paramNameToMatch, param);
                if (param instanceof SqlOutParameter) {
                    outParamNames.add(paramName);
                    if (isFunction() && !metaDataParamNames.contains(paramNameToMatch) && !returnDeclared) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Using declared out parameter '" + paramName +
                                    "' for function return value"
                            );
                        }
                        this.actualFunctionReturnName = paramName;
                        returnDeclared = true;
                    }
                }
            }
        }
        setOutParameterNames(outParamNames);
        List<SqlParameter> workParams = new ArrayList<>(declaredReturnParams);
        if (!provider.isProcedureColumnMetaDataUsed()) {
            workParams.addAll(declaredParams.values());
            return workParams;
        }
        Map<String, String> limitedInParamNamesMap = CollectionUtils.newHashMap(this.limitedInParameterNames.size());
        for (String limitedParamName : this.limitedInParameterNames) {
            limitedInParamNamesMap.put(lowerCase(provider.parameterNameToUse(limitedParamName)), limitedParamName);
        }
        for (CallParameterMetaData meta : provider.getCallParameterMetaData()) {
            String paramName = meta.getParameterName();
            String paramNameToCheck = null;
            if (paramName != null) {
                paramNameToCheck = lowerCase(provider.parameterNameToUse(paramName));
            }
            String paramNameToUse = provider.parameterNameToUse(paramName);
            if (declaredParams.containsKey(paramNameToCheck) || (meta.isReturnParameter() && returnDeclared)) {
                SqlParameter param;
                if (meta.isReturnParameter()) {
                    param = declaredParams.get(getFunctionReturnName());
                    if (param == null && !getOutParameterNames().isEmpty()) {
                        param = declaredParams.get(getOutParameterNames().get(0).toLowerCase());
                    }
                    if (param == null) {
                        throw new InvalidDataAccessApiUsageException(
                                "Unable to locate declared parameter for function return value - " +
                                        " add an SqlOutParameter with name '" + getFunctionReturnName() + "'"
                        );
                    } else {
                        this.actualFunctionReturnName = param.getName();
                    }
                } else {
                    param = declaredParams.get(paramNameToCheck);
                }
                if (param != null) {
                    workParams.add(param);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Using declared parameter for '" + (paramNameToUse != null ? paramNameToUse : getFunctionReturnName()) + "'");
                    }
                }
            } else {
                if (meta.isReturnParameter()) {
                    // DatabaseMetaData.procedureColumnReturn or possibly procedureColumnResult
                    if (!isFunction() && !isReturnValueRequired() && paramName != null &&
                            provider.byPassReturnParameter(paramName)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Bypassing meta-data return parameter for '" + paramName + "'");
                        }
                    } else {
                        String returnNameToUse = (StringUtils.hasLength(paramNameToUse) ? paramNameToUse : getFunctionReturnName());
                        workParams.add(provider.createDefaultOutParameter(returnNameToUse, meta));
                        if (isFunction()) {
                            this.actualFunctionReturnName = returnNameToUse;
                            outParamNames.add(returnNameToUse);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added meta-data return parameter for '" + returnNameToUse + "'");
                        }
                    }
                } else {
                    if (paramNameToUse == null) {
                        paramNameToUse = "";
                    }
                    if (meta.getParameterType() == DatabaseMetaData.procedureColumnOut) {
                        workParams.add(provider.createDefaultOutParameter(paramNameToUse, meta));
                        outParamNames.add(paramNameToUse);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added meta-data out parameter for '" + paramNameToUse + "'");
                        }
                    } else if (meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {
                        workParams.add(provider.createDefaultInOutParameter(paramNameToUse, meta));
                        outParamNames.add(paramNameToUse);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added meta-data in-out parameter for '" + paramNameToUse + "'");
                        }
                    } else {
                        // DatabaseMetaData.procedureColumnIn or possibly procedureColumnUnknown
                        if (this.limitedInParameterNames.isEmpty() || limitedInParamNamesMap.containsKey(lowerCase(paramNameToUse))) {
                            workParams.add(provider.createDefaultInParameter(paramNameToUse, meta));
                            if (logger.isDebugEnabled()) {
                                logger.debug("Added meta-data in parameter for '" + paramNameToUse + "'");
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Limited set of parameters " + limitedInParamNamesMap.keySet() +
                                        " skipped parameter for '" + paramNameToUse + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
        return workParams;
    }

    /**
     * 将输入参数值与声明要在调用中使用的参数匹配。
     *
     * @param parameterSource 输入值
     * @return 一个 Map 包含匹配的参数名称和从输入中获取的值
     */
    public Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
        // For parameter source lookups we need to provide case-insensitive lookup support
        // since the database meta-data is not necessarily providing case-sensitive parameter names.
        Map<String, String> caseInsensitiveParameterNames = SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
        Map<String, String> callParameterNames = CollectionUtils.newHashMap(this.callParameters.size());
        Map<String, Object> matchedParameters = CollectionUtils.newHashMap(this.callParameters.size());
        for (SqlParameter parameter : this.callParameters) {
            if (parameter.isInputValueProvided()) {
                String parameterName = parameter.getName();
                String parameterNameToMatch = obtainMetaDataProvider().parameterNameToUse(parameterName);
                if (parameterNameToMatch != null) {
                    callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
                }
                if (parameterName != null) {
                    if (parameterSource.hasValue(parameterName)) {
                        matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, parameterName));
                    } else {
                        String lowerCaseName = parameterName.toLowerCase();
                        if (parameterSource.hasValue(lowerCaseName)) {
                            matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
                        } else {
                            String englishLowerCaseName = parameterName.toLowerCase(Locale.ENGLISH);
                            if (parameterSource.hasValue(englishLowerCaseName)) {
                                matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, englishLowerCaseName));
                            } else {
                                String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(parameterName);
                                if (parameterSource.hasValue(propertyName)) {
                                    matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
                                } else {
                                    if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
                                        String sourceName = caseInsensitiveParameterNames.get(lowerCaseName);
                                        matchedParameters.put(parameterName, SqlParameterSourceUtils.getTypedValue(parameterSource, sourceName));
                                    } else if (logger.isInfoEnabled()) {
                                        logger.info("Unable to locate the corresponding parameter value for '" +
                                                parameterName + "' within the parameter values provided: " +
                                                caseInsensitiveParameterNames.values()
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Matching " + caseInsensitiveParameterNames.values() + " with " + callParameterNames.values());
            logger.debug("Found match for " + matchedParameters.keySet());
        }
        return matchedParameters;
    }

    /**
     * 将输入参数值与声明要在调用中使用的参数匹配
     *
     * @param inParameters 输入值
     * @return 一个 Map 包含匹配的参数名称和从输入中获取的值
     */
    public Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> inParameters) {
        CallMetaDataProvider provider = obtainMetaDataProvider();
        if (!provider.isProcedureColumnMetaDataUsed()) {
            return inParameters;
        }
        Map<String, String> callParameterNames = CollectionUtils.newHashMap(this.callParameters.size());
        for (SqlParameter parameter : this.callParameters) {
            if (parameter.isInputValueProvided()) {
                String parameterName = parameter.getName();
                String parameterNameToMatch = provider.parameterNameToUse(parameterName);
                if (parameterNameToMatch != null) {
                    callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
                }
            }
        }
        Map<String, Object> matchedParameters = CollectionUtils.newHashMap(inParameters.size());
        inParameters.forEach((parameterName, parameterValue) -> {
            String parameterNameToMatch = provider.parameterNameToUse(parameterName);
            String callParameterName = callParameterNames.get(lowerCase(parameterNameToMatch));
            if (callParameterName == null) {
                if (logger.isDebugEnabled()) {
                    Object value = parameterValue;
                    if (value instanceof SqlParameterValue) {
                        value = ((SqlParameterValue) value).getValue();
                    }
                    if (value != null) {
                        logger.debug("Unable to locate the corresponding IN or IN-OUT parameter for \"" +
                                parameterName + "\" in the parameters used: " + callParameterNames.keySet()
                        );
                    }
                }
            } else {
                matchedParameters.put(callParameterName, parameterValue);
            }
        });
        if (matchedParameters.size() < callParameterNames.size()) {
            for (String parameterName : callParameterNames.keySet()) {
                String parameterNameToMatch = provider.parameterNameToUse(parameterName);
                String callParameterName = callParameterNames.get(lowerCase(parameterNameToMatch));
                if (!matchedParameters.containsKey(callParameterName) && logger.isInfoEnabled()) {
                    logger.info("Unable to locate the corresponding parameter value for '" + parameterName +
                            "' within the parameter values provided: " + inParameters.keySet()
                    );
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Matching " + inParameters.keySet() + " with " + callParameterNames.values());
            logger.debug("Found match for " + matchedParameters.keySet());
        }
        return matchedParameters;
    }

    public Map<String, ?> matchInParameterValuesWithCallParameters(Object[] parameterValues) {
        Map<String, Object> matchedParameters = CollectionUtils.newHashMap(parameterValues.length);
        int i = 0;
        for (SqlParameter parameter : this.callParameters) {
            if (parameter.isInputValueProvided()) {
                String parameterName = parameter.getName();
                matchedParameters.put(parameterName, parameterValues[i++]);
            }
        }
        return matchedParameters;
    }

    /**
     * 根据配置和元数据信息构建调用字符串
     *
     * @return 要使用的调用字符串
     */
    public String createCallString() {
        Assert.state(this.metaDataProvider != null, "No CallMetaDataProvider available");
        StringBuilder callString;
        int parameterCount = 0;
        String catalogNameToUse;
        String schemaNameToUse;
        // For Oracle where catalogs are not supported we need to reverse the schema name
        // and the catalog name since the catalog is used for the package name
        if (this.metaDataProvider.isSupportsSchemasInProcedureCalls() && !this.metaDataProvider.isSupportsCatalogsInProcedureCalls()) {
            schemaNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
            catalogNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
        } else {
            catalogNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
            schemaNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
        }
        if (isFunction() || isReturnValueRequired()) {
            callString = new StringBuilder("{? = call ");
            parameterCount = -1;
        } else {
            callString = new StringBuilder("{call ");
        }
        if (StringUtils.hasLength(catalogNameToUse)) {
            callString.append(catalogNameToUse).append('.');
        }
        if (StringUtils.hasLength(schemaNameToUse)) {
            callString.append(schemaNameToUse).append('.');
        }
        callString.append(this.metaDataProvider.procedureNameToUse(getProcedureName()));
        callString.append('(');
        for (SqlParameter parameter : this.callParameters) {
            if (!parameter.isResultsParameter()) {
                if (parameterCount > 0) {
                    callString.append(", ");
                }
                if (parameterCount >= 0) {
                    callString.append(createParameterBinding(parameter));
                }
                parameterCount++;
            }
        }
        callString.append(")}");
        return callString.toString();
    }

    /**
     * 构建参数绑定片段
     *
     * @param parameter 调用参数
     * @return 参数绑定片段
     */
    protected String createParameterBinding(SqlParameter parameter) {
        return (isNamedBinding() ? parameter.getName() + " => ?" : "?");
    }

    private static String lowerCase(String paramName) {
        return (paramName != null ? paramName.toLowerCase() : "");
    }
}
