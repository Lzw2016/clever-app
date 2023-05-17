package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.utils.CodegenUtils;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/01 18:56 <br/>
 */
@Slf4j
public class CodegenUtilsTest {
    @Test
    public void t01() {
        Jdbc jdbc = BaseTest.newMysql();
        CodegenCodeConfig config = new CodegenCodeConfig()
                .setOutDir("../clever-task/src/main/java/org/clever/task/core/model")
                .setPackageName("org.clever.task.core.model")
                // .removeCodegenType(CodegenType.JAVA_ENTITY)
                // .removeCodegenType(CodegenType.JAVA_QUERYDSL)
                // .addCodegenType(CodegenType.GROOVY_ENTITY)
                // .addCodegenType(CodegenType.GROOVY_QUERYDSL)
                // .addCodegenType(CodegenType.KOTLIN_ENTITY)
                // .addCodegenType(CodegenType.KOTLIN_QUERYDSL)
                .addTablePrefix("task_")
                .addTable("task_shell_job")
                .addSchema("public")
                .addSchema("test")
                .addSchema("__occupyPosition");
        CodegenUtils.genCode(jdbc, config);
        log.info("-->");
        jdbc.close();
    }

    @Test
    public void t02() {
        Jdbc jdbc = BaseTest.newMysql();
        CodegenCodeConfig config = new CodegenCodeConfig()
                .setOutDir("../clever-data-jdbc/src/main/java/org/clever/data/jdbc/support")
                .setPackageName("org.clever.data.jdbc.support")
                .addTable("biz_code")
                .addTable("auto_increment_id")
                .addTable("sys_lock")
                .addSchema("public")
                .addSchema("test")
                .addSchema("__occupyPosition");
        CodegenUtils.genCode(jdbc, config);
        log.info("-->");
        jdbc.close();
    }
}
