package org.clever.core;

import lombok.extern.slf4j.Slf4j;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/18 23:11 <br/>
 */
@Slf4j
public class ClassLoaderTestA {
    private static final String a = "aaa";
    private static final String a2 = "aaa222";
    private static final String b = "bbb";
    private static final String b2 = "bbb222";

    public static void t01() {
        log.info("a={}", a);
        log.info("a2={}", a2);
    }

    public static void t02() {
        log.info("b={}", b);
        log.info("b2={}", b2);
    }
}
