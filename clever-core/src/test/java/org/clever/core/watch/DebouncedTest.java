package org.clever.core.watch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/30 14:19 <br/>
 */
@Slf4j
public class DebouncedTest {
    @Test
    public void t01() throws InterruptedException {
        Debounced<String> debounced = new Debounced<>(s -> log.info("# ----------> s = {}", s), 200);
        debounced.execute("111");
        debounced.execute("222");
        debounced.execute("333");
        debounced.execute("444");
        debounced.execute("555");
        debounced.execute("666");
        Thread.sleep(200);
        log.info("#------------------------------------------------------- 1");
        Thread.sleep(100);
        debounced.execute("777");
        log.info("#------------------------------------------------------- 2");
        Thread.sleep(150);
        debounced.execute("888");
        log.info("#------------------------------------------------------- 3");
        Thread.sleep(200);
        debounced.execute("999");
        log.info("#------------------------------------------------------- 4");
        Thread.sleep(250);
        debounced.execute("000");
        log.info("#------------------------------------------------------- 5");
        Thread.sleep(1000 * 5);
        log.info("#------------------------------------------------------- 6");
    }

    @Test
    public void t02() throws InterruptedException {
        Debounced<String> debounced = new Debounced<>(s -> log.info("# ----------> s = {}", s), 199);
        debounced.execute("111");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 1");
        debounced.execute("222");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 2");
        debounced.execute("333");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 3");
        debounced.execute("444");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 4");
        debounced.execute("555");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 5");
        debounced.execute("666");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 6");
        debounced.execute("777");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 7");
        debounced.execute("888");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 8");
        debounced.execute("999");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 9");
        debounced.execute("000");
        Thread.sleep(100);
        log.info("#------------------------------------------------------- 0");
        Thread.sleep(1000 * 3);
        debounced.execute("aaa");
        Thread.sleep(1000 * 3);
    }

    @Test
    public void t03() throws InterruptedException {
        Debounced<String> debounced = new Debounced<>(s -> log.info("# ----------> s = {}", s), 200);
        debounced.execute("111");
        debounced.execute("222");
        debounced.execute("333");
        debounced.execute("444");
        debounced.execute("555");
        debounced.execute("666");
        Thread.sleep(1000 * 3);
    }
}
