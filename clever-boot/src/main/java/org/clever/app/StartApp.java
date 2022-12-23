package org.clever.app;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.StartupInfoLogger;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.logging.LoggingBootstrap;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.exception.BusinessException;
import org.clever.web.WebServerBootstrap;
import org.clever.web.filter.EchoFilter;
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
        log.info("The following profiles are active: {}", StringUtils.join(environment.getActiveProfiles(), ", "));
        // 初始化非web资源

        // 创建web服务
        WebServerBootstrap webServerBootstrap = new WebServerBootstrap();
        // 注册 Filter
        OrderIncrement filterOrder = new OrderIncrement();
        webServerBootstrap.getFilterRegistrar()
                .addFilter(EchoFilter.create(environment), "/*", "EchoFilter")
                .addFilter(ctx -> {
                    log.info("### Filter_1_之前");
                    ctx.next();
                    log.info("### Filter_1_之后");
                }, "/*", "Filter_1", filterOrder.incrL1())
                // .addFilter(ctx -> {
                //     log.info("### Filter_2_之前");
                //     ctx.res.setContentType(ContentType.TEXT_PLAIN.getMimeType());
                //     ctx.res.getWriter().println("提前结束请求");
                //     ctx.res.setStatus(200);
                //     ctx.res.getWriter().flush();
                //     log.info("### Filter_2_之后");
                // }, "/*", "Filter_2", filterOrder.incrL1())
                .addFilter(ctx -> {
                    log.info("### Filter_3_之前");
                    ctx.next();
                    log.info("### Filter_3_之后");
                }, "/*", "Filter_3", filterOrder.incrL1());
        // 注册 Servlet
        OrderIncrement servletOrder = new OrderIncrement();
        webServerBootstrap.getServletRegistrar()
                .addServlet(ctx -> {
                    ctx.res.setContentType(ContentType.TEXT_PLAIN.getMimeType());
                    ctx.res.getWriter().println("自定义Servlet");
                    ctx.res.setStatus(200);
                    ctx.res.getWriter().flush();
                }, "/servlet/*", "Servlet_1", servletOrder.incrL1());
        // 注册 EventListener
        //  webServerBootstrap.getEventListenerRegistrar()
        //          .addEventListener()
        // 注册http前置(Before)处理器
        OrderIncrement beforeHandlerOrder = new OrderIncrement();
        webServerBootstrap.getHandlerRegistrar()
                .addBeforeHandler("*", beforeHandlerOrder.incrL1(), "Before_测试_1", ctx -> {
                    // 这里无法直接响应客户端请求(直接返回响应数据)
                    log.info("### Before_测试_1");
                });
        // 注册http后置(After)处理器
        OrderIncrement afterHandlerOrder = new OrderIncrement();
        webServerBootstrap.getHandlerRegistrar()
                .addAfterHandler("*", afterHandlerOrder.incrL1(), "After_测试_1", ctx -> {
                    log.info("### After_测试_1");
                    // ctx.status(200);
                    // ctx.result("After_测试_1");
                });
        // 注册websocket前置(Before)处理器
        // webServerBootstrap.getHandlerRegistrar()
        //         .addWsBeforeHandler()
        // 注册websocket后置(After)处理器
        // webServerBootstrap.getHandlerRegistrar()
        //         .addWsAfterHandler()
        // 注册插件
        OrderIncrement pluginOrder = new OrderIncrement();
        webServerBootstrap.getPluginRegistrar()
                .addPlugin(ExceptionHandlerPlugin.INSTANCE, "异常处理插件", pluginOrder.incrL1());
        // 注册 JavalinEventListener
        // webServerBootstrap.getJavalinEventListenerRegistrar()
        //         .addListener()
        // 初始化web服务
        Javalin javalin = webServerBootstrap.init(environment);
        AppContextHolder.registerBean("javalin", javalin, true);
        // 自定义请求处理
        javalin.get("/test", ctx -> {
            // 可多次读取body
            log.info("body --> {}", ctx.body());
            log.info("body --> {}", ctx.body());
            ctx.result("test,中文");
        });
        javalin.get("/test2", ctx -> {
            throw new BusinessException("服务端异常");
        });
        // javalin.error()
        // 启动web服务
        webServerBootstrap.start();
        // 系统关闭时的任务处理
        AppShutdownHook.addShutdownHook(javalin::stop, 0, "停止WebServer");
        AppShutdownHook.addShutdownHook(loggingBootstrap::destroy, Double.MAX_VALUE, "停止日志模块");
        // 系统启动完成日志
        startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime));
    }
}
