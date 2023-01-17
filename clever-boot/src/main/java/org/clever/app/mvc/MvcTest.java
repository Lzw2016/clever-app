package org.clever.app.mvc;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import lombok.Data;
import org.clever.core.http.CookieUtils;
import org.clever.util.MultiValueMap;
import org.clever.web.http.HttpStatus;
import org.clever.web.http.multipart.MultipartFile;
import org.clever.web.support.mvc.annotation.CookieValue;
import org.clever.web.support.mvc.annotation.RequestBody;
import org.clever.web.support.mvc.annotation.RequestHeader;
import org.clever.web.support.mvc.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
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

    // RequestParam | -> RequestParamMapMethodArgumentResolver
    public static Object t06(@RequestParam MultiValueMap<String, String> map_1, @RequestParam Map<String, String> map_2) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", map_1);
        res.put("b", map_2);
        return res;
    }

    public static Object t06_1(Context ctx) {
        List<UploadedFile> list = ctx.uploadedFiles();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", list.size());
        return res;
    }

    // RequestParam | -> RequestParamMapMethodArgumentResolver
    public static Object t06_2(@RequestParam Map<String, MultipartFile> map) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", map.keySet());
        res.put("b", map.size());
        return res;
    }

    @Data
    public static final class BodyParam {
        private String a;
        private Date b;
    }

    // RequestBody | RequestBodyMethodProcessor
    public static Object t07(@RequestBody BodyParam bodyParam) {
        return bodyParam;
    }

    // RequestPart | RequestPartMethodArgumentResolver
    public static Object t08() {
        // TODO 文件上传
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", 1);
        return res;
    }

    // RequestHeader | RequestHeaderMethodArgumentResolver
    public static Object t09(@RequestHeader String a) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a);
        return res;
    }

    // RequestHeader | RequestHeaderMapMethodArgumentResolver
    public static Object t10(@RequestHeader MultiValueMap<String, String> map_1, @RequestHeader Map<String, String> map_2) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", map_1);
        res.put("b", map_2);
        return res;
    }

    // CookieValue | CookieValueMethodArgumentResolver
    public static Object t11(@CookieValue(required = false) String a, Context ctx) {
        CookieUtils.setCookieForCurrentPath(ctx.res, "a", "时间: " + System.currentTimeMillis() + " | 特殊字符: ':, '");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a);
        return res;
    }
}
