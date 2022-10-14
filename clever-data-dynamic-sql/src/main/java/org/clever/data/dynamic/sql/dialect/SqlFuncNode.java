package org.clever.data.dynamic.sql.dialect;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/21 13:50 <br/>
 */
@Data
public class SqlFuncNode {
    /**
     * SQL函数名称
     */
    private final String funcName;
    /**
     * SQL函数参数
     */
    private final List<SqlFuncNodeParam> params = new ArrayList<>();

    // --------------------------------------------------------------------- toSql时的可控参数

    /**
     * 当funcName为空时,参数是否需用括号"()"包裹
     */
    @Getter
    @Setter
    private boolean paren = true;

    public SqlFuncNode(String funcName) {
        this.funcName = funcName;
    }

    public SqlFuncNode copy() {
        SqlFuncNode sqlFuncNode = new SqlFuncNode(this.funcName);
        for (SqlFuncNodeParam param : params) {
            sqlFuncNode.getParams().add(param.copy());
        }
        return sqlFuncNode;
    }
}
