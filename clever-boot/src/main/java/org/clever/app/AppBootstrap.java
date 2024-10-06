package org.clever.app;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppBasicsConfig;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.task.StartupTaskBootstrap;
import org.clever.data.jdbc.JdbcBootstrap;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.redis.RedisBootstrap;
import org.clever.security.SecurityBootstrap;
import org.clever.spring.boot.ConfigDataBootstrap;
import org.clever.spring.boot.LoggingBootstrap;
import org.clever.spring.boot.StartupInfoLogger;
import org.clever.task.TaskBootstrap;
import org.clever.task.ext.JsExecutorBootstrap;
import org.clever.web.MvcBootstrap;
import org.clever.web.PathConstants;
import org.clever.web.WebServerBootstrap;
import org.clever.web.config.WebConfig;
import org.clever.web.filter.*;
import org.clever.web.plugin.ExceptionHandlerPlugin;
import org.clever.web.plugin.NotFoundResponsePlugin;
import org.springframework.core.env.StandardEnvironment;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 17:25 <br/>
 */
@Slf4j
public abstract class AppBootstrap {
    public static void start(String[] args) {
        try {
            doStart(args);
        } catch (Throwable e) {
            log.info("启动失败!", e);
            System.exit(-1);
        }
    }

    private static void doStart(String[] args) {
        final long startTime = System.currentTimeMillis();
        // 读取系统配置
        final StandardEnvironment environment = new StandardEnvironment();
        final ConfigDataBootstrap configDataBootstrap = new ConfigDataBootstrap();
        configDataBootstrap.init(environment, args);
        // 初始化日志模块
        final LoggingBootstrap loggingBootstrap = new LoggingBootstrap();
        loggingBootstrap.init(environment);
        AppContextHolder.registerBean("environment", environment, true);
        AppContextHolder.registerBean("loggingBootstrap", loggingBootstrap, true);
        StartupInfoLogger startupInfoLogger = new StartupInfoLogger(StartApp.class);
        startupInfoLogger.logStarting(log);
        log.info("The following profiles are active: {}", StringUtils.join(environment.getActiveProfiles(), ", "));
        // 全局的资源根路径
        final AppBasicsConfig appBasicsConfig = AppBasicsConfig.create(environment);
        final String rootPath = appBasicsConfig.getRootPath();
        appBasicsConfig.init();
        // Jdbc初始化
        final JdbcBootstrap jdbcBootstrap = JdbcBootstrap.create(rootPath, environment);
        final JdbcConfig jdbcConfig = jdbcBootstrap.getJdbcConfig();
        jdbcBootstrap.init();
        // Redis初始化
        final RedisBootstrap redisBootstrap = RedisBootstrap.create(environment);
        redisBootstrap.init();
        // 创建web服务
        final WebServerBootstrap webServerBootstrap = WebServerBootstrap.create(rootPath, environment);
        final WebConfig webConfig = webServerBootstrap.getWebConfig();
        // mvc功能
        final MvcBootstrap mvcBootstrap = MvcBootstrap.create(rootPath, jdbcConfig.getDefaultName(), environment);
        // security功能
        final SecurityBootstrap securityBootstrap = SecurityBootstrap.create(environment);
        SecurityBootstrap.useDefaultSecurity(securityBootstrap.getSecurityConfig());
        // 注册 Filter
        final OrderIncrement filterOrder = new OrderIncrement();
        webServerBootstrap.getFilterRegistrar()
            .addFilter(ApplyConfigFilter.create(rootPath, webConfig), PathConstants.ALL, "ApplyConfigFilter", filterOrder.incrL1())
            .addFilter(EchoFilter.create(environment), PathConstants.ALL, "EchoFilter", filterOrder.incrL1())
            .addFilter(ExceptionHandlerFilter.INSTANCE, PathConstants.ALL, "ExceptionHandlerFilter", filterOrder.incrL1())
            .addFilter(GlobalRequestParamsFilter.INSTANCE, PathConstants.ALL, "GlobalRequestParamsFilter", filterOrder.incrL1())
            .addFilter(CorsFilter.create(environment), PathConstants.ALL, "CorsFilter", filterOrder.incrL1())
            .addFilter(mvcBootstrap.getMvcHandlerMethodFilter(), PathConstants.ALL, "MvcHandlerMethodFilter", filterOrder.incrL1())
            .addFilter(securityBootstrap.getAuthenticationFilter(), PathConstants.ALL, "AuthenticationFilter", filterOrder.incrL1())
            .addFilter(securityBootstrap.getLoginFilter(), PathConstants.ALL, "LoginFilter", filterOrder.incrL1())
            .addFilter(securityBootstrap.getLogoutFilter(), PathConstants.ALL, "LogoutFilter", filterOrder.incrL1())
            .addFilter(securityBootstrap.getAuthorizationFilter(), PathConstants.ALL, "AuthorizationFilter", filterOrder.incrL1())
            .addFilter(StaticResourceFilter.create(rootPath, environment), PathConstants.ALL, "StaticResourceFilter", filterOrder.incrL1())
            .addFilter(mvcBootstrap.getMvcFilter(), PathConstants.ALL, "MvcFilter", filterOrder.incrL1());
        // 注册 Servlet
        // OrderIncrement servletOrder = new OrderIncrement();
        // webServerBootstrap.getServletRegistrar()
        //         .addServlet();
        // 注册 EventListener
        // OrderIncrement listenerOrder = new OrderIncrement();
        //  webServerBootstrap.getEventListenerRegistrar()
        //          .addEventListener()
        // 注册http前置(Before)处理器
        // OrderIncrement beforeHandlerOrder = new OrderIncrement();
        // webServerBootstrap.getHandlerRegistrar()
        //         .addBeforeHandler();
        // 注册http后置(After)处理器
        // OrderIncrement afterHandlerOrder = new OrderIncrement();
        // webServerBootstrap.getHandlerRegistrar()
        //         .addAfterHandler();
        // 注册websocket前置(Before)处理器
        // webServerBootstrap.getHandlerRegistrar()
        //         .addWsBeforeHandler()
        // 注册websocket后置(After)处理器
        // webServerBootstrap.getHandlerRegistrar()
        //         .addWsAfterHandler()
        // 注册插件
        final OrderIncrement pluginOrder = new OrderIncrement();
        webServerBootstrap.getPluginRegistrar()
            .addPlugin(ExceptionHandlerPlugin.INSTANCE, "异常处理插件", pluginOrder.incrL1())
            .addPlugin(mvcBootstrap.getMvcFilter(), "MVC处理插件", pluginOrder.incrL1())
            .addPlugin(NotFoundResponsePlugin.INSTANCE, "404处理插件", pluginOrder.incrL1());
        // 注册 JavalinEventListener
        // OrderIncrement javalinListenerOrder = new OrderIncrement();
        // webServerBootstrap.getJavalinEventListenerRegistrar()
        //         .addListener()
        // 初始化web服务
        final Javalin javalin = webServerBootstrap.init();
        AppContextHolder.registerBean("javalin", javalin, true);
        // 自定义请求处理
        // javalin.get();
        // javalin.post();
        // 启动web服务
        webServerBootstrap.start();
        // 系统关闭时的任务处理
        AppShutdownHook.addShutdownHook(javalin::stop, OrderIncrement.NORMAL, "停止WebServer");
        AppShutdownHook.addShutdownHook(loggingBootstrap::destroy, Double.MAX_VALUE, "停止日志模块");
        // 启动开机任务
        final StartupTaskBootstrap startupTaskBootstrap = StartupTaskBootstrap.create(rootPath, environment);
        ClassLoader classLoader = AppContextHolder.getBean("hotReloadClassLoader", ClassLoader.class);
        if (classLoader != null) {
            startupTaskBootstrap.setClassLoader(classLoader);
        }
        startupTaskBootstrap.start();
        // 分布式定时任务
        final TaskBootstrap taskBootstrap = TaskBootstrap.create(rootPath, environment);
        JsExecutorBootstrap jsExecutorBootstrap = JsExecutorBootstrap.create(taskBootstrap.getSchedulerConfig(), environment);
        jsExecutorBootstrap.init();
        taskBootstrap.start();
        // 系统启动完成日志
        startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime));
    }
}
