package org.clever.jdbc.core;

import java.sql.Types;

/**
 * 表示从存储过程调用返回的更新计数。
 *
 * <p>与所有存储过程参数一样，返回的更新计数必须有名称。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:02 <br/>
 */
public class SqlReturnUpdateCount extends SqlParameter {
    /**
     * 创建一个新的SqlReturnUpdateCount。
     *
     * @param name 输入和输出Map中使用的参数名称
     */
    public SqlReturnUpdateCount(String name) {
        super(name, Types.INTEGER);
    }

    /**
     * 此实现始终返回 {@code false}.
     */
    @Override
    public boolean isInputValueProvided() {
        return false;
    }

    /**
     * 此实现始终返回 {@code true}.
     */
    @Override
    public boolean isResultsParameter() {
        return true;
    }
}
