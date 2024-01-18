package org.clever.js.nashorn;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.nashorn.utils.JSTools;
import org.clever.js.nashorn.utils.ScriptEngineUtils;
import org.junit.jupiter.api.Test;

import javax.script.Bindings;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/26 12:48 <br/>
 */
@Slf4j
public class JSToolsTest {

    @Test
    public void t01() {
        Bindings bindings = ScriptEngineUtils.newObject();
        String str = JSTools.inspect(bindings);
        log.info("# -> {}", str);
    }
}
