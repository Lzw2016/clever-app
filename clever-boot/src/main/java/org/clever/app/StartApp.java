package org.clever.app;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.StartupInfoLogger;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.logging.LoggingBootstrap;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.env.StandardEnvironment;
import org.clever.web.WebServerBootstrap;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 21:25 <br/>
 */
@Slf4j
public class StartApp {
    public static void main(String[] args) {
        final long startTime = System.currentTimeMillis();
        // 读取系统配置 & 初始化日志模块
        StandardEnvironment environment = new StandardEnvironment();
        LoggingBootstrap loggingBootstrap = new LoggingBootstrap(Thread.currentThread().getContextClassLoader());
        ConfigDataBootstrap configDataBootstrap = new ConfigDataBootstrap();
        configDataBootstrap.init(environment);
        loggingBootstrap.init(environment);
        AppContextHolder.registerBean("environment", environment, true);
        AppContextHolder.registerBean("loggingBootstrap", loggingBootstrap, true);
        StartupInfoLogger startupInfoLogger = new StartupInfoLogger(StartApp.class);
        startupInfoLogger.logStarting(log);
        // 启动web服务
        WebServerBootstrap webServerBootstrap = new WebServerBootstrap();
        webServerBootstrap.init(environment);
        Javalin javalin = webServerBootstrap.getJavalin();
        AppContextHolder.registerBean("javalin", javalin, true);
        startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime));
        // 优雅停机
        AppShutdownHook.addShutdownHook(javalin::stop, -100, "停止Web服务器");
        AppShutdownHook.addShutdownHook(loggingBootstrap::destroy, Integer.MAX_VALUE, "停止日志模块");
    }
}
