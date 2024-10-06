package org.clever.app.mvc;

import io.javalin.http.UploadedFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.clever.app.mapper.MapperTest;
import org.clever.core.DateUtils;
import org.clever.core.http.CookieUtils;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.response.R;
import org.clever.core.validator.annotation.IntStatus;
import org.clever.core.validator.annotation.NotBlank;
import org.clever.data.jdbc.DaoFactory;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.jdbc.support.ProcedureJdbcCall;
import org.clever.web.Context;
import org.clever.web.mvc.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Types;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 09:22 <br/>
 */
@Slf4j
public class MvcTest {
    // 无参数
    public static Object t01() {
        Entity1 entity1 = new Entity1();
        entity1.setAge(1);
        entity1.setName("ABC");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", 1);
        res.put("b", entity1);
        res.put("c", true);
        res.put("d", new Date());
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

    // RequestParam | -> RequestParamMapMethodArgumentResolver
    public static Object t06_3(@RequestParam MultiValueMap<String, MultipartFile> map) {
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
    public static Object t08(@RequestPart MultipartFile a) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a.getName());
        res.put("b", a.getSize());
        return res;
    }

    public static final String abc = "@@@###";

    // RequestHeader | RequestHeaderMethodArgumentResolver
    @SneakyThrows
    public static Object t09(@RequestHeader String a) {
        Thread.sleep(100);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a);
        res.put("abc", abc);
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
    public static Object t11(@CookieValue(required = false) String a, HttpServletResponse response) {
        CookieUtils.setCookieForCurrentPath(response, "a", "时间: " + System.currentTimeMillis() + " | 特殊字符: ':, '");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", a);
        return res;
    }

    @Data
    public static final class ParamEntity {
        @NotBlank
        @NotNull
        private String a;
        @IntStatus({1, 2, 3})
        private Integer b;
    }

    public static Object t12(@Validated @RequestBody ParamEntity param) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", param);
        return res;
    }

    @Transactional(datasource = {"mysql"})
    public static Object t13(Context ctx) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", ctx.path());
        return res;
    }

    @Transactional(datasource = {"postgresql"})
    public static Object t14(Context ctx) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("a", ctx.path());
        return res;
    }

    private static final Jdbc jdbc = DaoFactory.getJdbc("mysql");
    private static final QueryDSL queryDSL = DaoFactory.getQueryDSL("mysql");

    @Transactional
    public static Object t15(Context ctx) {
        Map<String, Object> res = new LinkedHashMap<>();
        // code_name, pattern,sequence,reset_flag
        res.put("code_name", "biz001");
        res.put("pattern", "CK${yyMMddHHmm}${seq}");
        res.put("sequence", 0);
        res.put("reset_flag", "yyMMdd");
        jdbc.insertTable("biz_code", res);
        return res;
    }

    public static Object t16(Context ctx) {
        Jdbc postgresql = DaoFactory.getJdbc("postgresql");
        Map<String, Object> params = new LinkedHashMap<>();
        // params.put("updateAt", new Date());
        params.put("updateAt", "2023-11-23 11:50:19");
        return postgresql.queryMany("select * from asn_in where update_at>=:updateAt", params);
    }

    @Transactional(disabled = true)
    public static Object t17(ParamEntity param) {
        QueryByPage queryByPage = QueryByPage.getCurrent();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("queryByPage", queryByPage);
        data.put("param", param);
        return data;
    }

//    @Transactional(disabled = true)
//    public static Object t18() {
//        TaskJobTrigger trigger = queryDSL.selectFrom(taskJobTrigger)
//            .where(taskJobTrigger.id.eq(1653059609297231874L))
//            .fetchOne();
//        trigger.setDescription("AAA");
//        queryDSL.update(
//            taskJobTrigger,
//            taskJobTrigger.id.eq(trigger.getId()),
//            trigger,
//            update -> update.set(taskJobTrigger.updateAt, queryDSL.currentDate()),
//            taskJobTrigger.fireCount,
//            taskJobTrigger.createAt,
//            taskJobTrigger.updateAt
//        );
//        return trigger;
//    }

    @SneakyThrows
    @Transactional(disabled = true)
    public static R<?> t19() {
        // "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        Date date = DateUtils.parseDate("2023-05-26 16:05:06.019", "yyyy-MM-dd HH:mm:ss.SSS");
        log.info("date -> {}", date);
        String str = DateFormatUtils.format(date, "yy|yyyy|MM|dd|HH|hh|mm|ss|SSS|D");
        log.info("str -> {}", str);
        // next_id('t01', 1, 0, 0)
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("seq_name", "t01");
        paramMap.put("size", 1);
        paramMap.put("step", 1);
        Map<String, Object> data1 = jdbc.callGet("next_ids", paramMap);
        Map<String, Object> data2 = jdbc.callGet("next_ids", "t01", 1, 1);
        Long id = jdbc.queryLong("select next_id('t01') from dual");
        return R.success(new Object[]{data1, data2, id});
    }

    @SneakyThrows
    @Transactional(disabled = true)
    public static R<?> t20() {
        Jdbc postgresql = DaoFactory.getJdbc("postgresql");
        // Map<String, Object> data = postgresql.callGet("next_codes", "RULE_QUALITY", 1);
        // Long id = jdbc.queryLong("select next_id('t05')");
        String sql = " " +
            "DO $$" +
            "DECLARE" +
            "    _res_1 varchar;" +
            "    _res_2 bool;" +
            "BEGIN" +
            "    select pg_advisory_lock(123) into _res_1;" +
            "    select pg_advisory_unlock(123) into _res_2;" +
            "END;" +
            "$$;";
        int count = postgresql.update(sql);
        // postgresql.getJdbcTemplate().getJdbcTemplate().execute(sql);
        return R.success(count);
    }

    @SneakyThrows
    @Transactional(disabled = true)
    public static R<?> t21() {
        Long a = jdbc.queryLong("select get_lock('lock001', 300) from dual");
        Long b = jdbc.queryLong("select release_lock('lock001') from dual");
        int c = "001".hashCode();
        return R.success(new Object[]{a, b, c});
    }

    @SneakyThrows
    @Transactional(disabled = true)
    public static R<?> t22() {
        Jdbc oracle = DaoFactory.getJdbc("oracle");
        SimpleJdbcCall request = new ProcedureJdbcCall(oracle)
            .withoutProcedureColumnMetaDataAccess()
            .withCatalogName("dbms_lock")
            .withFunctionName("request")
            .declareParameters(
                new SqlOutParameter("result", Types.INTEGER),
                new SqlParameter("lockhandle", Types.VARCHAR),
                new SqlParameter("lockmode", Types.TINYINT),
                new SqlParameter("timeout", Types.INTEGER)
                // new SqlParameter("release_on_commit", Types.BOOLEAN)
            );
        request.compile();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lockhandle", "123456");
        paramMap.put("lockmode", 6);
        paramMap.put("timeout", 0);
        // paramMap.put("release_on_commit", false);
        Integer res2 = request.executeFunction(Integer.class, paramMap);
        log.info("--> {}", res2);
        return R.success(new Object[]{"res", res2});
    }

    @SneakyThrows
    @Transactional(disabled = true)
    public static R<?> t23() {
        // mysql postgresql oracle
        Jdbc db = DaoFactory.getJdbc("oracle");
        Thread thread = new Thread(() -> {
            db.nativeTryLock("test", 3, locked -> {
                log.info("locked={}", locked);
            });
        });
        db.nativeLock("test", () -> {
            log.info("测试1");
            log.info("测试2");
            thread.start();
            try {
                Thread.sleep(1000 * 6);
            } catch (InterruptedException ignored) {
            }
        });
        thread.join();
        return R.success(new Object[]{"res"});
    }

//    public static R<?> t24() {
//        Jdbc db = DaoFactory.getJdbc("postgresql");
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("param", db.likeBoth("b_"));
//        Object res_1 = db.queryMany("select * from test where a like :param ", paramMap);
//        Object res_2 = queryDSL.selectFrom(taskJobTrigger)
//            .where(taskJobTrigger.id.eq(1653059609297231874L))
//            .where(queryDSL.likeBoth(taskJobTrigger.namespace, "a%b_c"))
//            .fetchOne();
//        return R.success(new Object[]{res_1, res_2});
//    }

    private static final MapperTest mapperTest = DaoFactory.getMapper(MapperTest.class);

    public static R<?> t25() {
        // List<Map<String, Object>> list = mapperTest.q01(1L);
        List<Map<String, Object>> list = mapperTest.q02("abc");
        return R.success(list);
    }
}
