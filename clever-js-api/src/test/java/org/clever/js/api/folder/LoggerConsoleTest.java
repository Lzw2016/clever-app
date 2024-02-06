package org.clever.js.api.folder;

import lombok.extern.slf4j.Slf4j;
import org.clever.js.api.internal.LoggerConsole;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 23:17 <br/>
 */
@Slf4j
public class LoggerConsoleTest {
    @Test
    public void t01() {
        LoggerConsole console = LoggerConsole.INSTANCE;
        String label = "aa";

        console.count();
        console.count(label);
        console.count();
        console.count(label);

        console.time();
        console.time(label);
        console.timeLog(null, "T111 ", "2222 ");
        console.timeLog(label, "T333 ", "444 ");
        console.timeEnd();
        console.timeEnd(label);
    }

    @Test
    public void t02() {
        LoggerConsole console = LoggerConsole.INSTANCE;
        console.log("111");
        console.trace("222");
        console.debug("333");
        console.info("444");
        console.warn("555");
        console.error("666");
    }
}
