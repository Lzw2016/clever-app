package org.clever.data.jdbc.support.sqlparser;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/12 11:05 <br/>
 */
@Data
@Accessors(chain = true)
public class CountSqlInfo {
    /**
     * count sql 内容
     */
    private String sql;
    /**
     * 是否排序
     */
    private boolean orderBy = true;

    public static CountSqlInfo newInstance() {
        return new CountSqlInfo();
    }
}
