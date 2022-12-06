package org.clever.jdbc.core;

import org.clever.dao.InvalidDataAccessApiUsageException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper 类，它根据 SQL 语句和一组参数声明有效地创建具有不同参数的多个 {@link CallableStatementCreator} 对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:03 <br/>
 */
public class CallableStatementCreatorFactory {
    /**
     * SQL调用字符串，参数改变时不会改变
     */
    private final String callString;
    /**
     * SqlParameter 对象列表。可能不是 {@code null}
     */
    private final List<SqlParameter> declaredParameters;
    private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private boolean updatableResults = false;

    /**
     * 创建新工厂。将需要通过 {@link #addParameter} 方法添加参数或没有参数
     *
     * @param callString SQL调用字符串
     */
    public CallableStatementCreatorFactory(String callString) {
        this.callString = callString;
        this.declaredParameters = new ArrayList<>();
    }

    /**
     * 使用给定的 SQL 和给定的参数创建一个新工厂
     *
     * @param callString         SQL调用字符串
     * @param declaredParameters {@link SqlParameter} 对象列表
     */
    public CallableStatementCreatorFactory(String callString, List<SqlParameter> declaredParameters) {
        this.callString = callString;
        this.declaredParameters = declaredParameters;
    }

    /**
     * 返回 SQL 调用字符串
     */
    public final String getCallString() {
        return this.callString;
    }

    /**
     * 添加一个新的声明参数。
     * <p>添加参数的顺序很重要。
     *
     * @param param 要添加到已声明参数列表的参数
     */
    public void addParameter(SqlParameter param) {
        this.declaredParameters.add(param);
    }

    /**
     * 设置是否使用返回特定类型 ResultSet 的准备语句。
     * 特定类型的结果集。
     *
     * @param resultSetType 结果集类型
     * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
     * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
     * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
     */
    public void setResultSetType(int resultSetType) {
        this.resultSetType = resultSetType;
    }

    /**
     * 设置是否使用能够返回可更新 ResultSets 的准备好的语句
     */
    public void setUpdatableResults(boolean updatableResults) {
        this.updatableResults = updatableResults;
    }

    /**
     * 给定这些参数，返回一个新的 CallableStatementCreator 实例。
     *
     * @param params 参数列表（可能是 {@code null}）
     */
    public CallableStatementCreator newCallableStatementCreator(Map<String, ?> params) {
        return new CallableStatementCreatorImpl(params != null ? params : new HashMap<>());
    }

    /**
     * 给定此参数映射器，返回一个新的 CallableStatementCreator 实例
     *
     * @param inParamMapper the ParameterMapper implementation that will return a Map of parameters
     */
    public CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
        return new CallableStatementCreatorImpl(inParamMapper);
    }

    /**
     * 此类返回的 CallableStatementCreator 实现
     */
    private class CallableStatementCreatorImpl implements CallableStatementCreator, SqlProvider, ParameterDisposer {
        private ParameterMapper inParameterMapper;
        private Map<String, ?> inParameters;

        /**
         * 创建一个新的 CallableStatementCreatorImpl
         *
         * @param inParamMapper 用于映射输入参数的 ParameterMapper 实现
         */
        public CallableStatementCreatorImpl(ParameterMapper inParamMapper) {
            this.inParameterMapper = inParamMapper;
        }

        /**
         * 创建一个新的 CallableStatementCreatorImpl
         *
         * @param inParams SqlParameter 对象列表
         */
        public CallableStatementCreatorImpl(Map<String, ?> inParams) {
            this.inParameters = inParams;
        }

        @Override
        public CallableStatement createCallableStatement(Connection con) throws SQLException {
            // If we were given a ParameterMapper, we must let the mapper do its thing to create the Map.
            if (this.inParameterMapper != null) {
                this.inParameters = this.inParameterMapper.createMap(con);
            } else {
                if (this.inParameters == null) {
                    throw new InvalidDataAccessApiUsageException("A ParameterMapper or a Map of parameters must be provided");
                }
            }
            CallableStatement cs = null;
            if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
                cs = con.prepareCall(callString);
            } else {
                cs = con.prepareCall(callString, resultSetType, updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
            }
            int sqlColIndx = 1;
            for (SqlParameter declaredParam : declaredParameters) {
                if (!declaredParam.isResultsParameter()) {
                    // So, it's a call parameter - part of the call string.
                    // Get the value - it may still be null.
                    Object inValue = this.inParameters.get(declaredParam.getName());
                    if (declaredParam instanceof ResultSetSupportingSqlParameter) {
                        // It's an output parameter: SqlReturnResultSet parameters already excluded.
                        // It need not (but may be) supplied by the caller.
                        if (declaredParam instanceof SqlOutParameter) {
                            if (declaredParam.getTypeName() != null) {
                                cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getTypeName());
                            } else {
                                if (declaredParam.getScale() != null) {
                                    cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getScale());
                                } else {
                                    cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType());
                                }
                            }
                            if (declaredParam.isInputValueProvided()) {
                                StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
                            }
                        }
                    } else {
                        // It's an input parameter; must be supplied by the caller.
                        if (!this.inParameters.containsKey(declaredParam.getName())) {
                            throw new InvalidDataAccessApiUsageException("Required input parameter '" + declaredParam.getName() + "' is missing");
                        }
                        StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
                    }
                    sqlColIndx++;
                }
            }
            return cs;
        }

        @Override
        public String getSql() {
            return callString;
        }

        @Override
        public void cleanupParameters() {
            if (this.inParameters != null) {
                StatementCreatorUtils.cleanupParameters(this.inParameters.values());
            }
        }

        @Override
        public String toString() {
            return "CallableStatementCreator: sql=[" + callString + "]; parameters=" + this.inParameters;
        }
    }
}
