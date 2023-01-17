package org.clever.app.mvc;

import io.javalin.http.Context;
import org.clever.util.MultiValueMap;
import org.clever.web.http.HttpStatus;
import org.clever.web.support.mvc.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
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

    // 参数 Context | -> ContextMethodArgumentResolver
    public static Object t02(Context ctx) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", ctx.headerMap());
        return res;
    }

    // 参数 HttpServletRequest MultipartRequest MultipartHttpServletRequest | -> ContextMethodArgumentResolver
    public static Object t03(HttpServletRequest request) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", request.getRequestURI());
        return res;
    }

    // 参数 HttpServletResponse | -> ServletResponseMethodArgumentResolver
    public static Object t04(HttpServletResponse response) {
        response.setStatus(HttpStatus.CREATED.value());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", 1);
        return res;
    }

    // RequestParam | -> RequestParamMethodArgumentResolver
    public static Object t05(@RequestParam(required = false) Integer a, @RequestParam(required = false) Date b) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a);
        res.put("b", b);
        return res;
    }

    // RequestParam | -> RequestParamMethodArgumentResolver
    public static Object t06(@RequestParam MultiValueMap<String, String> map_1, @RequestParam Map<String, String> map_2) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", map_1);
        res.put("b", map_2);
        return res;
    }
}
