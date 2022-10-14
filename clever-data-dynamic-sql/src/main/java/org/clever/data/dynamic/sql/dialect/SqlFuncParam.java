package org.clever.data.dynamic.sql.dialect;

import lombok.Data;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/11 17:37 <br/>
 */
@Data
public class SqlFuncParam implements Serializable {
    /**
     * 是否是变量
     */
    private final boolean variable;
    /**
     * 参数字面值(源码)
     * <pre>
     * 1.参数是静态值，就是静态值的源码
     * 2.参数是变量，就是变量的源码
     * </pre>
     */
    private final String literal;
    /**
     * 参数名
     * <pre>
     * 1.参数是静态值，就使用“literal(字面量)”
     * 2.参数是变量，就使用“name(参数名)”
     * </pre>
     */
    private final String name;
    /**
     * 参数值
     * <pre>
     * 1.参数是变量时，对应变量的值
     * 2.参数是静态值时为null
     * </pre>
     */
    private final Object value;

    public SqlFuncParam(boolean variable, String literal, String name, Object value) {
        this.variable = variable;
        this.literal = literal;
        this.name = name;
        this.value = value;
    }

    public SqlFuncParam copy() {
        return new SqlFuncParam(this.variable, this.literal, this.name, this.value);
    }
}
