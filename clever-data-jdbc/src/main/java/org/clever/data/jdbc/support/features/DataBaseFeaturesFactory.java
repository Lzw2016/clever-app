package org.clever.data.jdbc.support.features;

import org.clever.data.jdbc.Jdbc;
import org.clever.util.Assert;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/10 10:21 <br/>
 */
public class DataBaseFeaturesFactory {
    public static DataBaseFeatures getDataBaseFeatures(Jdbc jdbc) {
        Assert.notNull(jdbc, "参数 jdbc 不能为null");
        switch (jdbc.getDbType()) {
            case MYSQL:
                return new MySQLFeatures(jdbc);
            case POSTGRE_SQL:
                return new PostgreSQLFeatures(jdbc);
            case ORACLE:
            case ORACLE_12C:
                return new OracleFeatures(jdbc);
            default:
                throw new UnsupportedOperationException(String.format("当前数据库类型(%s)未实现DataBaseFeatures", jdbc.getDbType()));
        }
    }
}
