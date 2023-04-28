package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/27 23:13 <br/>
 */
@Slf4j
public class MySQLMetaDataTest {
    @Test
    public void t01() {
        Jdbc jdbc = BaseTest.newMysql();
        MySQLMetaData metaData = new MySQLMetaData(jdbc);
        List<Schema> schemas = metaData.getSchemas(null, null);
        log.info("--> {}", schemas);
        jdbc.close();
    }
}
