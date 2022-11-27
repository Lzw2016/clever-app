package org.clever.data.jdbc.support;

import lombok.Data;
import org.clever.data.dynamic.sql.dialect.DbType;

import java.io.Serializable;

@Data
public class JdbcInfo implements Serializable {
    /**
     * 驱动名称
     */
    private String driverClassName;
    /**
     * jdbc连接
     */
    private String jdbcUrl;
    /**
     * 是否自动提交事务
     */
    private boolean isAutoCommit;
    /**
     * 是否只读数据源
     */
    private boolean isReadOnly;
    /**
     * 数据库类型
     */
    private DbType dbType;
    /**
     * 是否已关闭
     */
    private boolean isClosed;
}
