package org.clever.app;

import lombok.extern.slf4j.Slf4j;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 21:25 <br/>
 */
@Slf4j
public class StartApp {
    public static void main(String[] args) {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        AppBootstrap.start(args);
    }
}
