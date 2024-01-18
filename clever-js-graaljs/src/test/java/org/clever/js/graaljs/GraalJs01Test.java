package org.clever.js.graaljs;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/03/28 12:17 <br/>
 */
@Slf4j
public class GraalJs01Test {

    private Context context;

    @BeforeEach
    public void before() {
        context = Context.create();
    }

    @AfterEach
    public void after() {
        context.close();
    }

    @Test
    public void t0() throws IOException {
        String js = "var add = function(x) { return 1 + x };";
        Source source = Source.newBuilder("js", "utf-8", "tmp.js").content(js).build();
        context.eval(source);
        long start = System.currentTimeMillis();
        Value sum = context.getBindings("js").getMember("add").execute(5);
        long took = System.currentTimeMillis() - start;
        log.info("耗时: {}ms", took); // 120ms
        log.info("sum = [{}]", sum);
    }

    @Test
    public void t1() throws IOException {
        Source source = Source.newBuilder("js", "utf-8", "SOURCE_1.js").content(JsSource.SOURCE_1).build();
        context.eval(source);
        log.info("warming up ...");
        boolean first = true;
        for (int i = 0; i < JsSource.WARM_UP; i++) {
            long start = System.currentTimeMillis();
            context.getBindings("js").getMember("primesMain").execute();
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms", (end - start)); // 1270ms
            }
        }
        long took = 0;
        for (int i = 0; i < JsSource.ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            context.getBindings("js").getMember("primesMain").execute();
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms", (took / (i + 1))); // 63ms
        }
    }

    @Test
    public void t2() throws IOException {
        Source source = Source.newBuilder("js", "utf-8", "SOURCE_2.js").content(JsSource.SOURCE_2).build();
        context.eval(source);
        final int a = 10000;
        final int b = 10000;
        log.info("warming up ...");
        boolean first = true;
        for (int i = 0; i < JsSource.WARM_UP; i++) {
            long start = System.currentTimeMillis();
            Object sum = context.getBindings("js").getMember("test").execute(a, b);
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms | sum=[{}]", (end - start), new BigDecimal(sum.toString()).toPlainString()); // 1499ms | sum=[499999995000]
            }
        }
        long took = 0;
        for (int i = 0; i < JsSource.ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            Object sum = context.getBindings("js").getMember("test").execute(a, b);
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms | sum=[{}]", (took / (i + 1)), new BigDecimal(sum.toString()).toPlainString()); // 83ms | sum=[499999995000]
        }
    }

    @Test
    public void t3() throws IOException {
        Source source = Source.newBuilder("js", "utf-8", "SOURCE_3.js").content(JsSource.SOURCE_3).build();
        context.eval(source);
        log.info("warming up ...");
        boolean first = true;
        for (int i = 0; i < JsSource.WARM_UP; i++) {
            long start = System.currentTimeMillis();
            context.getBindings("js").getMember("test").execute();
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms", (end - start)); // 1428ms
            }
        }
        long took = 0;
        for (int i = 0; i < JsSource.ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            Object sum = context.getBindings("js").getMember("test").execute();
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms | sum=[{}]", (took / (i + 1)), new BigDecimal(sum.toString()).toPlainString()); // 69ms | sum=[1000001]
        }
    }
}
