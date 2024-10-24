package org.clever.web.mvc;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/25 23:09 <br/>
 */
@Slf4j
public class HttpRouteRegisterTest {

    @Test
    public void t01() {
        String clazz_1 = "org.clever.jscript.groovy.GroovyMain1";
        String clazz_2 = "org.clever.jscript.groovy.GroovyMain2";

        HttpRouteRegistry registry = new HttpRouteRegistry();
        registry.post("/aa", clazz_1, "aa")
            .get("/bb", clazz_1, "bb")
            .put("/cc", clazz_2, "cc")
            .delete("/dd", clazz_2, "dd")

            .startClass(clazz_1, "/ee")
            .post("/aa", "aa")
            .get("/dd", "bb")
            .delete("/cc", "cc")
            .endClass()

            .startBasePath("/ff")
            .post("/bb", clazz_2, "bb")
            .get("/cc", clazz_2, "cc")
            .delete("/dd", clazz_2, "dd")
            .endBasePath()

            .startClass(RouteInfo.class, "/gg")
            .post("/aa", "aa")
            .get("/dd", "bb")
            .delete("/cc", "cc")
            .endClass()

            .get("/gg", clazz_1, "gg")
        ;
        registry.printAllRouteInfo();
    }
}
