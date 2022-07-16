package org.clever.app;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.StartupInfoLogger;
import org.clever.boot.context.config.ConfigDataBootstrap;
import org.clever.boot.context.logging.LoggingBootstrap;
import org.clever.core.AppContextHolder;
import org.clever.core.env.StandardEnvironment;

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
        // 启动web服务
        Javalin javalin = Javalin.create(config -> {
//            // HTTP
//            config.autogenerateEtags = false;                                           // generate etags for responses
//            config.prefer405over404 = false;                                            // return 405 instead of 404 if path is mapped to different HTTP method
//            config.enforceSsl = false;                                                  // redirect all http requests to https
//            config.defaultContentType = "text/plain";                                   // the default content type
//            config.maxRequestSize = 1_000_000L;                                         // either increase this or use inputstream to handle large requests
//            config.asyncRequestTimeout = 0L;                                            // timeout in milliseconds for async requests (0 means no timeout)
//            config.addSinglePageRoot("/path", "/file");                                 // fancy 404 handler that returns the specified file for 404s on /path
//            config.addSinglePageRoot("/path", "/file", location);                       // fancy 404 handler that returns the specified file for 404s on /path
//            config.addSinglePageHandler("/path", handler);                              // fancy 404 handler that runs the specified Handler for 404s on /path
//            config.addStaticFiles("/directory", location);                              // add static files in directory at location (Location.CLASSPATH/Location.EXTERNAL)
//            config.addStaticFiles(staticFileConfig);                                    // add static files by StaticFileConfig, see Static Files section
//            config.enableWebjars();                                                     // add static files though webjars
//            config.enableCorsForAllOrigins();                                           // enable CORS for all origins
//            config.enableCorsForOrigin("origin1", "origin2");                           // enable CORS the specified origins
//            config.enableDevLogging();                                                  // enable dev logging (extensive debug logging meant for development)
//            config.registerPlugin(myPlugin);                                            // register a plugin
//            config.requestLogger((ctx, timeInMs) -> {                                   // register a request logger
//            });
//            // WebSocket
//            config.wsFactoryConfig((factory) -> {                                       // configure the Jetty WebSocketServletFactory
//            });
//            config.wsLogger((ws) -> {                                                   // register a WebSocket logger
//            });
//            // Server
//            config.ignoreTrailingSlashes = true;                                        // treat '/path' and '/path/' as the same path
//            config.contextPath = "/";                                                   // the context path (ex '/blog' if you are hosting an app on a subpath, like 'mydomain.com/blog')
//            config.server(() -> Server());                                              // set the Jetty Server
//            config.sessionHandler(() -> SessionHandler());                              // set the Jetty SessionHandler
//            config.configureServletContextHandler(handler -> {                          // configure the Jetty ServletContextHandler
//            });
//            config.jsonMapper(jsonMapper);                                              // configure Javalin's JsonMapper
//            // Misc
//            config.showJavalinBanner = true;                                            // show the glorious Javalin banner on startup
        }).events(eventListener -> {
            eventListener.serverStarting(() -> startupInfoLogger.logStarting(log));
            eventListener.serverStarted(() -> startupInfoLogger.logStarted(log, Duration.ofMillis(System.currentTimeMillis() - startTime)));
        }).start(7070);
        AppContextHolder.registerBean("javalin", javalin, true);
        // 优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            javalin.stop();
            loggingBootstrap.destroy();
        }));
    }
}
