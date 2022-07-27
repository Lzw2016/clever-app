package org.clever.jdbc.core;

import org.clever.dao.InvalidDataAccessApiUsageException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * {@link PreparedStatementSetter}的简单适配器，应用给定的参数数组和JDBC参数类型。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:11 <br/>
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {
    private final Object[] args;
    private final int[] argTypes;

    /**
     * 为给定参数创建新的ArgTypePreparedStatementSetter。
     *
     * @param args     要设置的参数
     * @param argTypes 参数的相应SQL类型
     */
    public ArgumentTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        if ((args != null && argTypes == null)
                || (args == null && argTypes != null)
                || (args != null && args.length != argTypes.length)) {
            throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
        }
        this.args = args;
        this.argTypes = argTypes;
    }

    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
        int parameterPosition = 1;
        if (this.args != null && this.argTypes != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
                    Collection<?> entries = (Collection<?>) arg;
                    for (Object entry : entries) {
                        if (entry instanceof Object[]) {
                            Object[] valueArray = ((Object[]) entry);
                            for (Object argValue : valueArray) {
                                doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
                                parameterPosition++;
                            }
                        } else {
                            doSetValue(ps, parameterPosition, this.argTypes[i], entry);
                            parameterPosition++;
                        }
                    }
                } else {
                    doSetValue(ps, parameterPosition, this.argTypes[i], arg);
                    parameterPosition++;
                }
            }
        }
    }

    /**
     * 使用传入的值和类型为准备好的语句的指定参数位置设置值。如果需要，此方法可以由子类重写。
     *
     * @param ps                PreparedStatement
     * @param parameterPosition 参数位置索引
     * @param argType           参数类型
     * @param argValue          参数值
     * @throws SQLException 如果由PreparedStatement方法引发
     */
    protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue) throws SQLException {
        StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
    }

    @Override
    public void cleanupParameters() {
        StatementCreatorUtils.cleanupParameters(this.args);
    }
}
