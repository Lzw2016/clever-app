package org.clever.task.core;

import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.DaoFactory;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/02 09:31 <br/>
 */
public class TaskDataSource {
    /**
     * security 模块使用的 jdbc 数据源
     */
    public static String JDBC_DATA_SOURCE_NAME;

    public static Jdbc getJdbc() {
        if (StringUtils.isBlank(JDBC_DATA_SOURCE_NAME)) {
            return DaoFactory.getJdbc();
        } else {
            return DaoFactory.getJdbc(JDBC_DATA_SOURCE_NAME);
        }
    }

    public static QueryDSL getQueryDSL() {
        if (StringUtils.isBlank(JDBC_DATA_SOURCE_NAME)) {
            return DaoFactory.getQueryDSL();
        } else {
            return DaoFactory.getQueryDSL(JDBC_DATA_SOURCE_NAME);
        }
    }
}
