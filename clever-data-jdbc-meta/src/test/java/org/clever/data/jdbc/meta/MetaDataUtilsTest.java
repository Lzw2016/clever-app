package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.codegen.CodegenType;
import org.clever.data.jdbc.meta.model.Schema;
import org.clever.data.jdbc.meta.model.Table;
import org.clever.data.jdbc.meta.utils.CodegenUtils;
import org.clever.data.jdbc.meta.utils.MetaDataUtils;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 11:34 <br/>
 */
@Slf4j
public class MetaDataUtilsTest {
    @Test
    public void t01() {
        Jdbc jdbc = BaseTest.newPostgresql();
        Table table = MetaDataUtils.getTable(jdbc, MetaDataUtils.currentSchema(jdbc), "auto_increment_id");
        log.info("--> {}", table);
        jdbc.close();
    }

    @Test
    public void t02() {
        // Jdbc jdbc = BaseTest.newPostgresql();
        Jdbc jdbc = BaseTest.newMysql();
        CodegenCodeConfig config = new CodegenCodeConfig()
                .setOutDir("./src/test/java/org/clever/model")
//                .setOutDir("./src/test/groovy/org/clever/model")
//                .setOutDir("./src/test/kotlin/org/clever/model")
                .setPackageName("org.clever.model")
//                .removeCodegenType(CodegenType.JAVA_ENTITY)
//                .removeCodegenType(CodegenType.JAVA_QUERYDSL)
//                .addCodegenType(CodegenType.GROOVY_ENTITY)
//                .addCodegenType(CodegenType.GROOVY_QUERYDSL)
//                .addCodegenType(CodegenType.KOTLIN_ENTITY)
//                .addCodegenType(CodegenType.KOTLIN_QUERYDSL)
                .addSchema("public")
                .addSchema("test")
//                .addTable("auto_increment_id")
                .addSchema("__occupyPosition");
        CodegenUtils.genCode(jdbc, config);
        log.info("-->");
        jdbc.close();
    }

    @Test
    public void t03() {
        Jdbc jdbc = BaseTest.newMysql();
        CodegenCodeConfig config = new CodegenCodeConfig()
                .setOutDir("./src/test/resources/doc")
                .setPackageName("org.clever.model")
                .removeCodegenType(CodegenType.JAVA_ENTITY)
                .removeCodegenType(CodegenType.JAVA_QUERYDSL)
                .addCodegenType(CodegenType.DB_DOC_MARKDOWN)
                .addCodegenType(CodegenType.DB_DOC_HTML)
                .addCodegenType(CodegenType.DB_DOC_WORD)
                .addSchema("public")
                .addSchema("test")
                .addSchema("__occupyPosition");
        CodegenUtils.genCode(jdbc, config);
        log.info("-->");
        jdbc.close();
    }

    @Test
    public void t04() {
        Jdbc jdbc = BaseTest.newPostgresql();
        Schema schema = MetaDataUtils.getSchema(jdbc);
        log.info("--> {}", schema.getTables().size());
        jdbc.close();
    }

    @Test
    public void t05() {
        Jdbc jdbc = BaseTest.newOracle();
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        Table table = metaData.getTable("wms8dev", "sys_user3");
        // log.info("--> \n\n{}\n", JacksonMapper.getInstance().toJson(table));
        log.info("--> \n\n{}\n", metaData.createTable(table));
        // log.info("--> \n\n{}\n", metaData.alterTable(table, table));
        jdbc.close();
    }

    @Test
    public void t06() {
        Jdbc jdbc = BaseTest.newMysql();
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        Table table = metaData.getTable("test", "auto_increment_id");
        // log.info("--> \n\n{}\n", JacksonMapper.getInstance().toJson(table));
        log.info("--> \n\n{}\n", metaData.createTable(table));
        // log.info("--> \n\n{}\n", metaData.alterTable(table, table));
        jdbc.close();
    }

    @Test
    public void t07() {
        Jdbc jdbc = BaseTest.newPostgresql();
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        Table table = metaData.getTable("public", "auto_increment_id");
        // log.info("--> \n\n{}\n", JacksonMapper.getInstance().toJson(table));
        log.info("--> \n\n{}\n", metaData.createTable(table));
        // log.info("--> \n\n{}\n", metaData.alterTable(table, table));
        jdbc.close();
    }

    @Test
    public void t08() {
        Jdbc jdbc = BaseTest.newOracle();
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        Table table = metaData.getTable("wms8dev", "sys_user3");
        MySQLMetaData mysqlMetaData = new MySQLMetaData(jdbc);
        // log.info("--> \n\n{}\n", JacksonMapper.getInstance().toJson(table));
        log.info("--> \n\n{}\n", mysqlMetaData.createTable(table));
        // log.info("--> \n\n{}\n", metaData.alterTable(table, table));
        jdbc.close();
    }

    @Test
    public void t09() {
        Jdbc jdbc = BaseTest.newPostgresql();
        AbstractMetaData metaData = MetaDataUtils.createMetaData(jdbc);
        Table table = metaData.getTable("public", "auto_increment_id2");
        MySQLMetaData mysqlMetaData = new MySQLMetaData(jdbc);
        // log.info("--> \n\n{}\n", JacksonMapper.getInstance().toJson(table));
        log.info("--> \n\n{}\n", mysqlMetaData.createTable(table));
        // log.info("--> \n\n{}\n", metaData.alterTable(table, table));
        jdbc.close();
    }
}
