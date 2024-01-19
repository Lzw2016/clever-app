package org.clever.js.javet;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.executors.IV8Executor;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.IV8ValueFunction;
import lombok.extern.slf4j.Slf4j;
import org.clever.js.javet.support.JavetLogger;
import org.junit.jupiter.api.Test;

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
            v8Runtime.setLogger(JavetLogger.Instance);
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
            nodeRuntime.setLogger(JavetLogger.Instance);
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
}
