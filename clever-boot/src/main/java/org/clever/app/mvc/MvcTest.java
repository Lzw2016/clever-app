package org.clever.app.mvc;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 09:22 <br/>
 */
public class MvcTest {
    // 无参数
    public static Object t01() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", 1);
        res.put("b", "abc");
        res.put("c", true);
        return res;
    }

    // 参数 Context
    public static Object t02(Context ctx) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", 1);
        res.put("b", ctx.headerMap());
        res.put("c", true);
        return res;
    }
}
