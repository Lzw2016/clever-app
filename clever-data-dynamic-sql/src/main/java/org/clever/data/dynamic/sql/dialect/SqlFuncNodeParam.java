package org.clever.data.dynamic.sql.dialect;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/21 13:50 <br/>
 */
@Data
public class SqlFuncNodeParam {
    /**
     * 参数类型
     */
    private final SqlFuncNodeParamEnum type;
    /**
     * SQL参数
     */
    private final SqlFuncParam param;
    /**
     * 子函数
     */
    private final SqlFuncNode func;

    public SqlFuncNodeParam(SqlFuncParam param) {
        this.type = SqlFuncNodeParamEnum.SQL_PARAM;
        this.param = param;
        this.func = null;
    }

    public SqlFuncNodeParam(SqlFuncNode func) {
        this.type = SqlFuncNodeParamEnum.SQL_FUNC;
        this.param = null;
        this.func = func;
    }

    private SqlFuncNodeParam(SqlFuncNodeParamEnum type, SqlFuncParam param, SqlFuncNode func) {
        this.type = type;
        this.param = param;
        this.func = func;
    }

    public SqlFuncNodeParam copy() {
        SqlFuncParam param = null;
        if (this.param != null) {
            param = this.param.copy();
        }
        SqlFuncNode func = null;
        if (this.func != null) {
            func = this.func.copy();
        }
        return new SqlFuncNodeParam(this.type, param, func);
    }
}
