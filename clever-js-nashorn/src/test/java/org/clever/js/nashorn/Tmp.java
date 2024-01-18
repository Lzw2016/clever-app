package org.clever.js.nashorn;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.nashorn.utils.ScriptEngineUtils;
import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/24 22:07 <br/>
 */
@Slf4j
public class Tmp {
    @Test
    public void t01() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Thread thread = new Thread(() -> {
            while (true) {
                atomicInteger.incrementAndGet();
            }
        });
        thread.setDaemon(true);
        executorService.execute(thread);
        Thread.sleep(100);
        thread.interrupt();
        for (int i = 0; i < 10; i++) {
            log.info("atomicInteger #1-> {}", atomicInteger.get());
            Thread.sleep(100);
        }
        executorService.shutdown();
        for (int i = 0; i < 10; i++) {
            log.info("atomicInteger #2 -> {}", atomicInteger.get());
            Thread.sleep(100);
        }
    }

    @Test
    public void t02() throws ScriptException {
        NashornScriptEngine engine = ScriptEngineUtils.getDefaultEngine();
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("x", 100);
        String jsCode = "(function() { var a = 1 + x; return a;});";
        Bindings bindings = new SimpleBindings();
        bindings.put("x", 10);
        ScriptObjectMirror function = (ScriptObjectMirror) engine.eval(jsCode, bindings);
        Object res = function.call(null);
        log.info("### res1 -> {}", res);
        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("x", 200);
        res = function.call(null);
        log.info("### res2 -> {}", res);
    }
}
