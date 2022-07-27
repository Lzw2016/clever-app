package org.clever.jdbc.core;

import org.clever.dao.InvalidDataAccessApiUsageException;

import java.sql.*;
import java.util.*;

/**
 * Helper类，该类基于一条SQL语句和一组参数声明，有效地创建具有不同参数的多个{@link PreparedStatementCreator}对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 00:01 <br/>
 */
public class PreparedStatementCreatorFactory {
    /**
     * SQL，当参数更改时不会更改
     */
    private final String sql;
    /**
     * SqlParameter对象列表(不能为空)
     */
    private final List<SqlParameter> declaredParameters;
    private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
    private boolean updatableResults = false;
    private boolean returnGeneratedKeys = false;
    private String[] generatedKeysColumnNames;

    /**
     * 创建新工厂。需要通过{@link #addParameter}方法添加参数或没有参数。
     *
     * @param sql 要执行的SQL语句
     */
    public PreparedStatementCreatorFactory(String sql) {
        this.sql = sql;
        this.declaredParameters = new ArrayList<>();
    }

    /**
     * 使用给定的SQL和JDBC类型创建一个新工厂。
     *
     * @param sql   要执行的SQL语句
     * @param types JDBC类型的int数组
     */
    public PreparedStatementCreatorFactory(String sql, int... types) {
        this.sql = sql;
        this.declaredParameters = SqlParameter.sqlTypesToAnonymousParameterList(types);
    }

    /**
     * 使用给定的SQL和参数创建一个新工厂。
     *
     * @param sql                要执行的SQL语句
     * @param declaredParameters {@link SqlParameter}对象列表
     */
    public PreparedStatementCreatorFactory(String sql, List<SqlParameter> declaredParameters) {
        this.sql = sql;
        this.declaredParameters = declaredParameters;
    }

    /**
     * 返回要执行的SQL语句。
     */
    public final String getSql() {
        return this.sql;
    }

    /**
     * 添加新声明的参数。
     * <p>参数添加的顺序很重要。
     *
     * @param param 要添加到已声明参数列表中的参数
     */
    public void addParameter(SqlParameter param) {
        this.declaredParameters.add(param);
    }

    /**
     * 设置是否使用返回特定类型ResultSets的准备语句。
     *
     * @param resultSetType ResultSet类型
     * @see ResultSet#TYPE_FORWARD_ONLY
     * @see ResultSet#TYPE_SCROLL_INSENSITIVE
     * @see ResultSet#TYPE_SCROLL_SENSITIVE
     */
    public void setResultSetType(int resultSetType) {
        this.resultSetType = resultSetType;
    }

    /**
     * 设置是否使用能够返回可更新ResultSets的准备语句。
     */
    public void setUpdatableResults(boolean updatableResults) {
        this.updatableResults = updatableResults;
    }

    /**
     * 设置准备好的语句是否能够返回自动生成的键。
     */
    public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
        this.returnGeneratedKeys = returnGeneratedKeys;
    }

    /**
     * 设置自动生成键的列名。
     */
    public void setGeneratedKeysColumnNames(String... names) {
        this.generatedKeysColumnNames = names;
    }

    /**
     * 为给定参数返回新的PreparedStatementSetter。
     *
     * @param params 参数列表（可能为空）
     */
    public PreparedStatementSetter newPreparedStatementSetter(List<?> params) {
        return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
    }

    /**
     * 为给定参数返回新的PreparedStatementSetter。
     *
     * @param params 参数数组（可能为空）
     */
    public PreparedStatementSetter newPreparedStatementSetter(Object[] params) {
        return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
    }

    /**
     * 返回给定参数的新PreparedStatementCreator。
     *
     * @param params 参数列表(可能为空)
     */
    public PreparedStatementCreator newPreparedStatementCreator(List<?> params) {
        return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
    }

    /**
     * 返回给定参数的新PreparedStatementCreator。
     *
     * @param params 参数数组（可能为空）
     */
    public PreparedStatementCreator newPreparedStatementCreator(Object[] params) {
        return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
    }

    /**
     * 返回给定参数的新PreparedStatementCreator。
     *
     * @param sqlToUse 要使用的实际SQL语句（如果与工厂的不同，例如由于命名参数扩展）
     * @param params   参数数组（可能为空）
     */
    public PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, Object[] params) {
        return new PreparedStatementCreatorImpl(
                sqlToUse, params != null ? Arrays.asList(params) : Collections.emptyList()
        );
    }

    /**
     * 此类返回的PreparedStatementCreator实现。
     */
    private class PreparedStatementCreatorImpl implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider, ParameterDisposer {
        private final String actualSql;
        private final List<?> parameters;

        public PreparedStatementCreatorImpl(List<?> parameters) {
            this(sql, parameters);
        }

        public PreparedStatementCreatorImpl(String actualSql, List<?> parameters) {
            this.actualSql = actualSql;
            this.parameters = parameters;
            if (parameters.size() != declaredParameters.size()) {
                // Account for named parameters being used multiple times
                Set<String> names = new HashSet<>();
                for (int i = 0; i < parameters.size(); i++) {
                    Object param = parameters.get(i);
                    if (param instanceof SqlParameterValue) {
                        names.add(((SqlParameterValue) param).getName());
                    } else {
                        names.add("Parameter #" + i);
                    }
                }
                if (names.size() != declaredParameters.size()) {
                    throw new InvalidDataAccessApiUsageException(
                            "SQL [" + sql + "]: given " + names.size()
                                    + " parameters but expected " + declaredParameters.size()
                    );
                }
            }
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps;
            if (generatedKeysColumnNames != null || returnGeneratedKeys) {
                if (generatedKeysColumnNames != null) {
                    ps = con.prepareStatement(this.actualSql, generatedKeysColumnNames);
                } else {
                    ps = con.prepareStatement(this.actualSql, PreparedStatement.RETURN_GENERATED_KEYS);
                }
            } else if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
                ps = con.prepareStatement(this.actualSql);
            } else {
                ps = con.prepareStatement(
                        this.actualSql,
                        resultSetType,
                        updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY
                );
            }
            setValues(ps);
            return ps;
        }

        @Override
        public void setValues(PreparedStatement ps) throws SQLException {
            // Set arguments: Does nothing if there are no parameters.
            int sqlColIndx = 1;
            for (int i = 0; i < this.parameters.size(); i++) {
                Object in = this.parameters.get(i);
                SqlParameter declaredParameter;
                // SqlParameterValue overrides declared parameter meta-data, in particular for
                // independence from the declared parameter position in case of named parameters.
                if (in instanceof SqlParameterValue) {
                    SqlParameterValue paramValue = (SqlParameterValue) in;
                    in = paramValue.getValue();
                    declaredParameter = paramValue;
                } else {
                    if (declaredParameters.size() <= i) {
                        throw new InvalidDataAccessApiUsageException(
                                "SQL [" + sql + "]: unable to access parameter number " + (i + 1)
                                        + " given only " + declaredParameters.size() + " parameters"
                        );
                    }
                    declaredParameter = declaredParameters.get(i);
                }
                if (in instanceof Iterable && declaredParameter.getSqlType() != Types.ARRAY) {
                    Iterable<?> entries = (Iterable<?>) in;
                    for (Object entry : entries) {
                        if (entry instanceof Object[]) {
                            Object[] valueArray = (Object[]) entry;
                            for (Object argValue : valueArray) {
                                StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, argValue);
                            }
                        } else {
                            StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, entry);
                        }
                    }
                } else {
                    StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, in);
                }
            }
        }

        @Override
        public String getSql() {
            return sql;
        }

        @Override
        public void cleanupParameters() {
            StatementCreatorUtils.cleanupParameters(this.parameters);
        }

        @Override
        public String toString() {
            return "PreparedStatementCreator: sql=[" + sql + "]; parameters=" + this.parameters;
        }
    }
}
