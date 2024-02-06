package org.clever.js.v8;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interfaces.IJavetBiConsumer;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.JavetCallbackContext;
import com.caoccao.javet.interop.executors.IV8Executor;
import com.caoccao.javet.utils.JavetResourceUtils;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.IV8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.v8.support.V8Logger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/19 10:48 <br/>
 */
@SuppressWarnings("CallToPrintStackTrace")
@Slf4j
public class TmpTest {
    @Test
    public void t01() {
        String jsCode = "(function() { var b = x; return 1 + b;});";
        try {
            V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
            v8Runtime.setLogger(V8Logger.INSTANCE);
            IV8Executor executor = v8Runtime.getExecutor(jsCode);
            V8Value v8Value = executor.execute();
            log.info("v8Value -> {}", v8Value);
            if (v8Value instanceof IV8ValueFunction) {
                IV8ValueFunction function = (IV8ValueFunction) v8Value;
                v8Runtime.getGlobalObject().set("x", 10);
                log.info("call -> {}", function.call(function));
                v8Runtime.getGlobalObject().set("x", 100);
                log.info("call -> {}", function.call(function));
            }
            v8Value.close();
            IV8ValueFunction function = executor.execute();
            log.info("call -> {}", function.call(function));
            function.close();
            // log.info("call -> {}", function.call(function));
            v8Runtime.close();
        } catch (JavetException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void t02() {
        String jsCode = "(function() { var b = x; return 1 + b;});";
        try {
            NodeRuntime nodeRuntime = V8Host.getNodeInstance().createV8Runtime();
            nodeRuntime.setLogger(V8Logger.INSTANCE);
            IV8Executor executor = nodeRuntime.getExecutor(jsCode);
            IV8ValueFunction function = executor.execute();
            log.info("v8Value -> {}", function);
            nodeRuntime.getGlobalObject().set("x", 10);
            log.info("call -> {}", function.call(function));
            nodeRuntime.getGlobalObject().set("x", 100);
            log.info("call -> {}", function.call(function));
            function.close();
            nodeRuntime.close();
        } catch (JavetException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    @Test
    public void t03() {
        String jsCode = "({a: 'aa', b: 100, c: false, d: {e: new Date, f: 1.1}});";
        V8Host v8Host = V8Host.getV8Instance();
        V8Runtime v8Runtime = v8Host.createV8Runtime();
        v8Runtime.setLogger(V8Logger.INSTANCE);

        log.info("Version -> {}", v8Runtime.getVersion());

        IV8Executor executor = v8Runtime.getExecutor(jsCode);
        V8ValueObject value = executor.execute();
        log.info("value -> {}", value);

        List<String> keys = value.getOwnPropertyNameStrings();
        log.info("keys -> {}", keys);
        value.forEach(key -> log.info("key -> {}", key));

        List<V8Value> values = new ArrayList<>();
        int count = value.forEach((IJavetBiConsumer<? extends V8Value, ? extends V8Value, ? extends Throwable>) (k, v) -> {
            log.info("{} -> {}", k, v);
            values.add(v);
        });
        log.info("count -> {}", count);
        log.info("values -> {}", values);
        values.forEach(v -> log.info("{} -> {}", v, v.isClosed()));

        V8ValueObject d = value.get("d");
        log.info("d -> {}", d);

        d.close();
        JavetResourceUtils.safeClose(value);
        v8Runtime.close();
    }

    @SneakyThrows
    @Test
    public void t04() {
        String jsCode = "(function(_java_m) { return _java_m();});";
        V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
        v8Runtime.setLogger(V8Logger.INSTANCE);
        IV8Executor executor = v8Runtime.getExecutor(jsCode);
        IV8ValueFunction function = executor.execute();

        JavetCallbackContext call = new JavetCallbackContext(
            "t01",
            new Inner(),
            Inner.class.getMethod("t01")
        );
        V8ValueFunction _java_m = v8Runtime.createV8ValueFunction(call);
        String res = function.callString(function, _java_m);
        log.info("res -> {}", res);

        v8Runtime.removeCallbackContext(call.getHandle());
        JavetResourceUtils.safeClose(_java_m);
        JavetResourceUtils.safeClose(function);
        JavetResourceUtils.safeClose(v8Runtime);
    }
}

@Slf4j
class Inner {
    public String t01() {
        log.info("@@@###");
        return "abc";
    }
}
