package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/11/20 14:31 <br/>
 */
@Slf4j
public class OracleMetaDataTest {
    @Test
    public void t01() {
        Jdbc jdbc = BaseTest.newOracle();
        OracleMetaData metaData = new OracleMetaData(jdbc);
        List<Schema> schemas = metaData.getSchemas(null, null);
        log.info("--> {}", schemas);
        jdbc.close();
    }

    @Test
    public void t02() {
        Jdbc jdbc = BaseTest.newOracle();
        OracleMetaData metaData = new OracleMetaData(jdbc);
        Table table = metaData.getTable(metaData.currentSchema(), "sys_jwt_token");
        log.info("--> {}", table);
        jdbc.close();
    }

    @Test
    public void t03() {
        Jdbc jdbc = BaseTest.newOracle();
        OracleMetaData metaData = new OracleMetaData(jdbc);
        List<Schema> schemas = metaData.getSchemas(null, null);
        log.info("--> \n{}", metaData.createSequence(schemas.get(1).getSequence("SEQ_ASN_IN")));
        log.info("--> \n{}", metaData.dropProcedure(schemas.get(1).getProcedures().get(0)));
        log.info("--> \n{}", metaData.createProcedure(schemas.get(1).getProcedures().get(0)));
        jdbc.close();
    }
}
