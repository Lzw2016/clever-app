package org.clever.data.jdbc.support.features;

import org.clever.core.Assert;
import org.clever.data.jdbc.Jdbc;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/10 10:21 <br/>
 */
public class DataBaseFeaturesFactory {
    public static DataBaseFeatures getDataBaseFeatures(Jdbc jdbc) {
        Assert.notNull(jdbc, "参数 jdbc 不能为null");
        return switch (jdbc.getDbType()) {
            case MYSQL -> new MySQLFeatures(jdbc);
            case POSTGRE_SQL -> new PostgreSQLFeatures(jdbc);
            case ORACLE, ORACLE_12C -> new OracleFeatures(jdbc);
            default -> throw new UnsupportedOperationException(String.format("当前数据库类型(%s)未实现DataBaseFeatures", jdbc.getDbType()));
        };
    }
}
