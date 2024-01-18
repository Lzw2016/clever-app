package org.clever.js.nashorn.utils;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/19 19:45 <br/>
 */
@Slf4j
public class ScriptEngineUtilsTest {

    @Test
    public void t01() {
        ScriptObjectMirror v1 = ScriptEngineUtils.newBoolean(false);
        ScriptObjectMirror v2 = ScriptEngineUtils.newObject();
        ScriptObjectMirror v3 = ScriptEngineUtils.newNumber(1);
        ScriptObjectMirror v4 = ScriptEngineUtils.newString("abc");
        log.info("### -> v1={} | v2={} | v3={} | v4={}", v1, v2, v3, v4);
        log.info("### -> v1={} | v2={} | v3={} | v4={}", ScriptEngineUtils.typeof(v1), ScriptEngineUtils.typeof(v2), ScriptEngineUtils.typeof(v3), ScriptEngineUtils.typeof(v4));
    }
}
