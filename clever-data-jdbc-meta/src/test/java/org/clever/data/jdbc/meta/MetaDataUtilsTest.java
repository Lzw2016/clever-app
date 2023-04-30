package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.codegen.CodegenType;
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
                .addSchema("public")
                .addSchema("test")
                .addSchema("__occupyPosition");
        CodegenUtils.genCode(jdbc, config);
        log.info("-->");
        jdbc.close();
    }
}
