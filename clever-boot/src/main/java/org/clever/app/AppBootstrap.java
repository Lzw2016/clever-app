package org.clever.app;

import io.javalin.Javalin;
import io.javalin.plugin.json.JsonMapper;
import io.javalin.plugin.json.JsonMapperKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.StartupInfoLogger;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.logging.LoggingBootstrap;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.OrderIncrement;
import org.clever.core.env.StandardEnvironment;
import org.clever.core.task.StartupTaskBootstrap;
import org.clever.data.jdbc.JdbcBootstrap;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.config.MybatisConfig;
import org.clever.data.redis.RedisBootstrap;
import org.clever.data.redis.config.RedisConfig;
import org.clever.web.JavalinAttrKey;
import org.clever.web.PathConstants;
import org.clever.web.WebServerBootstrap;
import org.clever.web.config.WebConfig;
import org.clever.web.filter.*;
import org.clever.web.plugin.ExceptionHandlerPlugin;
import org.clever.web.plugin.NotFoundResponsePlugin;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/20 17:25 <br/>
 */
@Slf4j
public abstract class AppBootstrap {
    public static void start() {
        try {
            doStart();
        } catch (Throwable e) {
            log.info("系统启动失败!!!", e);
            System.exit(-1);
        }
    }

    private static void doStart() {
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
        // 全局的资源根路径
        final String rootPath = environment.getProperty("rootPath");
        AppContextHolder.registerBean("rootPath", rootPath, true);
        // Jdbc初始化
        MybatisConfig mybatisConfig = Binder.get(environment).bind(MybatisConfig.PREFIX, MybatisConfig.class).orElseGet(MybatisConfig::new);
        JdbcConfig jdbcConfig = Binder.get(environment).bind(JdbcConfig.PREFIX, JdbcConfig.class).orElseGet(JdbcConfig::new);
        AppContextHolder.registerBean("mybatisConfig", mybatisConfig, true);
        AppContextHolder.registerBean("jdbcConfig", jdbcConfig, true);
        JdbcBootstrap jdbcBootstrap = new JdbcBootstrap(rootPath, jdbcConfig, mybatisConfig);
        jdbcBootstrap.init();
        // Redis初始化
        RedisConfig redisConfig = Binder.get(environment).bind(RedisConfig.PREFIX, RedisConfig.class).orElseGet(RedisConfig::new);
        AppContextHolder.registerBean("redisConfig", redisConfig, true);
        RedisBootstrap redisBootstrap = new RedisBootstrap(redisConfig);
        redisBootstrap.init();
        // 创建web服务
        WebServerBootstrap webServerBootstrap = WebServerBootstrap.create(environment);
        final WebConfig webConfig = webServerBootstrap.getWebConfig();
        // 注册 Filter
        OrderIncrement filterOrder = new OrderIncrement();
        MvcFilter mvcFilter = MvcFilter.create(rootPath, environment);
        webServerBootstrap.getFilterRegistrar()
                .addFilter(ApplyConfigFilter.create(rootPath, webConfig.getHttp()), PathConstants.ALL, "ApplyConfigFilter", filterOrder.incrL1())
                .addFilter(EchoFilter.create(environment), PathConstants.ALL, "EchoFilter", filterOrder.incrL1())
                .addFilter(ExceptionHandlerFilter.INSTANCE, PathConstants.ALL, "ExceptionHandlerFilter", filterOrder.incrL1())
                .addFilter(CorsFilter.create(environment), PathConstants.ALL, "CorsFilter", filterOrder.incrL1())
                .addFilter(StaticResourceFilter.create(rootPath, environment), "/*", "StaticResourceFilter", filterOrder.incrL1())
                .addFilter(mvcFilter, PathConstants.ALL, "MvcFilter", filterOrder.incrL1());
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
        OrderIncrement pluginOrder = new OrderIncrement();
        webServerBootstrap.getPluginRegistrar()
                .addPlugin(ExceptionHandlerPlugin.INSTANCE, "异常处理插件", pluginOrder.incrL1())
                .addPlugin(mvcFilter, "MVC处理插件", pluginOrder.incrL1())
                .addPlugin(NotFoundResponsePlugin.INSTANCE, "404处理插件", pluginOrder.incrL1());
        // 注册 JavalinEventListener
        // OrderIncrement javalinListenerOrder = new OrderIncrement();
        // webServerBootstrap.getJavalinEventListenerRegistrar()
        //         .addListener()
        // 初始化web服务
        Javalin javalin = webServerBootstrap.init();
        AppContextHolder.registerBean("javalin", javalin, true);
        JsonMapper jsonMapper = javalin.attribute(JsonMapperKt.JSON_MAPPER_KEY);
        if (jsonMapper != null) {
            AppContextHolder.registerBean("javalinJsonMapper", jsonMapper, true);
            AppContextHolder.registerBean("javalinObjectMapper", javalin._conf.inner.appAttributes.get(JavalinAttrKey.JACKSON_OBJECT_MAPPER), true);
        }
        // 自定义请求处理
        // javalin.get();
        // javalin.post();
        // TODO WebSocket未实现
        javalin.ws("/ws", wsConfig -> {
            wsConfig.onConnect(ctx -> {
            });
            wsConfig.onMessage(ctx -> {
            });
            wsConfig.onBinaryMessage(ctx -> {
            });
            wsConfig.onClose(ctx -> {
            });
            wsConfig.onError(ctx -> {
            });
        });
        // 启动web服务
        webServerBootstrap.start();
        // 系统关闭时的任务处理
        AppShutdownHook.addShutdownHook(javalin::stop, OrderIncrement.NORMAL, "停止WebServer");
        AppShutdownHook.addShutdownHook(loggingBootstrap::destroy, Double.MAX_VALUE, "停止日志模块");
        // 系统启动完成日志
        startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime));
        // 启动开机任务
        StartupTaskBootstrap startupTaskBootstrap = StartupTaskBootstrap.create(rootPath, environment);
        ClassLoader classLoader = AppContextHolder.getBean("hotReloadClassLoader", ClassLoader.class);
        if (classLoader != null) {
            startupTaskBootstrap.setClassLoader(classLoader);
        }
        startupTaskBootstrap.start();
    }
}
