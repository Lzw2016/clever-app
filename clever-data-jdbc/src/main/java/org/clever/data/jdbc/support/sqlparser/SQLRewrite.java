package org.clever.data.jdbc.support.sqlparser;

import org.clever.core.tuples.TupleTwo;

import java.util.HashMap;
import java.util.Map;

/**
 * sql重写接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/13 11:46 <br/>
 */
public interface SQLRewrite {
    /**
     * 重写sql语句
     *
     * @param rawSql 原始sql语句
     * @param params 原始sql参数
     * @return {@code TupleTwo<重写后sql, 新增的扩展参数>}
     */
    TupleTwo<String, Map<String, Object>> rewrite(String rawSql, Object params);

    /**
     * 重写sql语句
     *
     * @param rawSql 原始sql语句
     * @return {@code TupleTwo<重写后sql, 新增的扩展参数>}
     */
    default TupleTwo<String, Map<String, Object>> rewrite(String rawSql) {
        return rewrite(rawSql, new HashMap<>());
    }
}
