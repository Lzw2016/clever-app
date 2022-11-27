package org.clever.data.jdbc.support;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/02/24 15:22 <br/>
 */
@Data
public class DbColumnMetaData {
    private String tableName;
    private String columnName;
    private String columnTypeName;
    private Integer columnType;
}
