package org.clever.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 应用给定参数数组的{@link PreparedStatementSetter}的简单适配器。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:10 <br/>
 */
public class ArgumentPreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {
    private final Object[] args;

    /**
     * 为给定参数创建新的ArgPreparedStatementSetter。
     *
     * @param args 要设置的参数
     */
    public ArgumentPreparedStatementSetter(Object[] args) {
        this.args = args;
    }

    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
        if (this.args != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                doSetValue(ps, i + 1, arg);
            }
        }
    }

    /**
     * 使用传入的值为指定参数索引的准备语句设置值。如果需要，此方法可以由子类重写。
     *
     * @param ps                PreparedStatement
     * @param parameterPosition 参数位置索引
     * @param argValue          要设置的值
     * @throws SQLException 如果由PreparedStatement方法引发
     */
    protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
        if (argValue instanceof SqlParameterValue) {
            SqlParameterValue paramValue = (SqlParameterValue) argValue;
            StatementCreatorUtils.setParameterValue(ps, parameterPosition, paramValue, paramValue.getValue());
        } else {
            StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
        }
    }

    @Override
    public void cleanupParameters() {
        StatementCreatorUtils.cleanupParameters(this.args);
    }
}
