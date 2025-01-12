package org.clever.data.jdbc.support.sqlparser;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 23:15 <br/>
 */
@Slf4j
public class JSqlParserCountSqlOptimizerTest {
    @Test
    public void test01() {
        final String absolutePath = new File("./src/test/resources/performance_test").getAbsolutePath();
        FileSystemMyBatisMapperSql mybatisMapperSql = new FileSystemMyBatisMapperSql(absolutePath);
        mybatisMapperSql.reloadAll();
        CountSqlOptimizer countSqlOptimizer = new JSqlParserCountSqlOptimizer();
        SqlSource sqlSource = mybatisMapperSql.getSqlSource("queryAllArea", "bas/area/Area.xml", DbType.MYSQL);
        String sql = sqlSource.getBoundSql(DbType.MYSQL, new HashMap<>()).getSql();
        log.info("sql=\n{}", sql);
        sql = countSqlOptimizer.getCountSql(sql);
        log.info("sql=\n{}", sql);
    }
}
