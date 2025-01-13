package org.clever.data.jdbc.support.sqlparser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 18:22 <br/>
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CountSqlOptions {
    /**
     * 使用sql解析来优化count sql
     */
    private boolean optimizeCountSql = true;
    /**
     * sql解析时，是否优化join子句
     */
    private boolean optimizeJoin = false;
}
