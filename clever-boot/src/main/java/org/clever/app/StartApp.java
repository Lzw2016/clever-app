package org.clever.app;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.StartupInfoLogger;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.logging.LoggingBootstrap;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.exception.BusinessException;
import org.clever.web.WebServerBootstrap;
import org.clever.web.plugin.ExceptionHandlerPlugin;

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
        // 创建web服务
        WebServerBootstrap webServerBootstrap = new WebServerBootstrap();
        // 注册http前置(Before)处理器
        OrderIncrement beforeHandlerOrder = new OrderIncrement();
        webServerBootstrap.getHandlerRegistrar()
//                .addBeforeHandler("*", ctx-> {
//                    ctx.result("OK");
//                    ctx.res.getOutputStream().close();
//                })
                .addBeforeHandler("*", beforeHandlerOrder.incrL1(), "测试_1", ctx -> {
                    log.info("### 1 start");
                    // Thread.sleep(1000 * 3);
                    log.info("### 1 end");
                });
//                .addBeforeHandler("*", ctx -> log.info("### 1"))
//                .addBeforeHandler("*", ctx -> log.info("### 2"));
        // 注册http后置(After)处理器
        // OrderIncrement afterHandlerOrder = new OrderIncrement();
        // 注册websocket前置(Before)处理器
        // 注册websocket后置(After)处理器
        // 注册插件
        OrderIncrement pluginOrder = new OrderIncrement();
        webServerBootstrap.getPluginRegistrar()
                .addPlugin(ExceptionHandlerPlugin.INSTANCE, "异常处理插件", pluginOrder.incrL1());
        // 初始化并启动web服务
        Javalin javalin = webServerBootstrap.init(environment);
        AppContextHolder.registerBean("javalin", javalin, true);
        // 自定义请求处理
        javalin.post("/test", ctx -> {
            log.info("body --> {}", ctx.body());
            log.info("body --> {}", ctx.body());
            ctx.result("test,中文");
        });
        javalin.get("/test2", ctx -> {
            throw new BusinessException("服务端异常");
        });
//        javalin.error()
        // 优雅停机
        AppShutdownHook.addShutdownHook(javalin::stop, 0, "停止WebServer");
        AppShutdownHook.addShutdownHook(loggingBootstrap::destroy, Integer.MAX_VALUE, "停止日志模块");
        // 系统启动完成日志
        startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime));
    }
}
