package org.clever.data.jdbc.mybatis;

import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/07 09:24 <br/>
 */
public class ComposeMyBatisMapperSql implements MyBatisMapperSql {
    @Override
    public SqlSource getSqlSource(String sqlId, String stdXmlPath, DbType dbType, String... projects) {
        return null;
    }

    @Override
    public void startWatch(int period) {

    }

    @Override
    public void stopWatch() {

    }

    @Override
    public boolean idWatch() {
        return false;
    }
}
