package org.clever.data.jdbc.meta.model;

import lombok.Data;

import java.util.List;

/**
 * 查询sql的元数据
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2025/01/14 11:42 <br/>
 */
@Data
public class QueryMetaData {
    /**
     * 查询SQL语句
     */
    private String sql;
    /**
     * 查询字段元数据
     */
    private List<QueryFieldMetaData> selectFields;
}
// all tables       sql中出现表
// all from tables  from子句中出现的表(可以查询字段值的表)
/*
{
    sql: "",
    selectFields: [],
}
*/
