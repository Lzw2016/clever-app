package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 11:35 <br/>
 */
@Slf4j
public class PostgreSQLMetaDataTest {
    @Test
    public void t03() {
        Jdbc jdbc = BaseTest.newPostgresql();
        PostgreSQLMetaData metaData = new PostgreSQLMetaData(jdbc);
        List<Schema> schemas = metaData.getSchemas(null, null);
        log.info("--> {}", schemas);
        jdbc.close();
    }

    @Test
    public void t04() {
        Jdbc jdbc = BaseTest.newPostgresql();
        PostgreSQLMetaData metaData = new PostgreSQLMetaData(jdbc);
        Table table = metaData.getTable(metaData.currentSchema(), "auto_increment_id");
        log.info("--> {}", table);
        jdbc.close();
    }
}
