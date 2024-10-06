package org.clever.js.v8;

import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.executors.IV8Executor;
import com.caoccao.javet.values.reference.V8ValueFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.clever.js.v8.support.V8Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/19 14:59 <br/>
 */
@Slf4j
public class V801Test {
    public static final int WARM_UP = 15;
    public static final int ITERATIONS = 30;

    @SneakyThrows
    @Test
    public void t01() {
        String source = FileUtils.readFileToString(new File("../clever-js-api/src/test/resources/performance01.js"), StandardCharsets.UTF_8);
        V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
        v8Runtime.setLogger(V8Logger.INSTANCE);
        IV8Executor executor = v8Runtime.getExecutor(source);
        executor.executeVoid();
        V8ValueFunction function = v8Runtime.getGlobalObject().get("primesMain");
        log.info("function -> {}", function);
        boolean first = true;
        for (int i = 0; i < WARM_UP; i++) {
            long start = System.currentTimeMillis();
            function.callVoid(function);
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms", (end - start)); // 26ms
            }
        }
        long took = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            function.callVoid(function);
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms", (took / (i + 1))); // 25ms
        }
        function.close();
        v8Runtime.close();
    }

    @SneakyThrows
    @Test
    public void t02() {
        String source = FileUtils.readFileToString(new File("../clever-js-api/src/test/resources/performance02.js"), StandardCharsets.UTF_8);
        V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
        v8Runtime.setLogger(V8Logger.INSTANCE);
        IV8Executor executor = v8Runtime.getExecutor(source);
        executor.executeVoid();
        V8ValueFunction function = v8Runtime.getGlobalObject().get("test");
        log.info("function -> {}", function);
        final int a = 10000;
        final int b = 10000;
        boolean first = true;
        for (int i = 0; i < WARM_UP; i++) {
            long start = System.currentTimeMillis();
            Object sum = function.call(function, a, b);
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms | sum=[{}]", (end - start), new BigDecimal(sum.toString()).toPlainString()); // 51ms | sum=[499999995000]
            }
        }
        long took = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            Object sum = function.call(function, a, b);
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms | sum=[{}]", (took / (i + 1)), new BigDecimal(sum.toString()).toPlainString()); // 46ms | sum=[499999995000]
        }
        function.close();
        v8Runtime.close();
    }


    @SneakyThrows
    @Test
    public void t03() {
        String source = FileUtils.readFileToString(new File("../clever-js-api/src/test/resources/performance03.js"), StandardCharsets.UTF_8);
        V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
        v8Runtime.setLogger(V8Logger.INSTANCE);
        IV8Executor executor = v8Runtime.getExecutor(source);
        executor.executeVoid();
        V8ValueFunction function = v8Runtime.getGlobalObject().get("test");
        log.info("function -> {}", function);
        boolean first = true;
        for (int i = 0; i < WARM_UP; i++) {
            long start = System.currentTimeMillis();
            function.callVoid(function);
            long end = System.currentTimeMillis();
            if (first) {
                first = false;
                log.info("第一次耗时: {}ms", (end - start)); // 22ms
            }
        }
        long took = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.currentTimeMillis();
            Integer sum = function.callInteger(function);
            took += System.currentTimeMillis() - start;
            log.info("耗时: {}ms | sum=[{}]", (took / (i + 1)), new BigDecimal(sum.toString()).toPlainString()); // 28ms | sum=[1000001]
        }
        function.close();
        v8Runtime.close();
    }
}
