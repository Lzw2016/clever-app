package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.jetty.JettyUtil;
import io.javalin.plugin.json.JavalinJackson;
import lombok.Getter;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.env.Environment;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.util.Assert;
import org.clever.web.config.*;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

/**
 * 用于初始化和启动web服务器的工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 12:35 <br/>
 */
public class WebServerBootstrap {
    protected volatile boolean initialized = false;
    @Getter
    private final JavalinPluginRegistrar pluginRegistrar = new JavalinPluginRegistrar();
    @Getter
    private final JavalinHandlerRegistrar handlerRegistrar = new JavalinHandlerRegistrar();

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        Assert.notNull(environment, "environment 不能为空");
        WebConfig webConfig = Binder.get(environment).bind(WebConfig.PREFIX, WebConfig.class).orElseGet(WebConfig::new);
        Javalin javalin = Javalin.create(config -> {
            // 初始化http相关配置
            HttpConfig http = webConfig.getHttp();
            initHTTP(config, http);
            // 初始化Server相关配置
            ServerConfig server = webConfig.getServer();
            initServer(config, server);
            // 初始化WebSocket相关配置
            WebSocketConfig webSocket = webConfig.getWebSocketConfig();
            initWebSocket(config, webSocket);
            // 初始化杂项配置
            MiscConfig misc = webConfig.getMisc();
            initMisc(config, misc);
            // 自定义 JsonMapper
            JacksonConfig jackson = webConfig.getJackson();
            config.jsonMapper(new JavalinJackson(initJackson(jackson)));
            // 注册自定义插件
            pluginRegistrar.init(config);
            // 自定义配置
            if (configCallback != null) {
                configCallback.accept(config);
            }
        });
        // 注册自定义Handler
        handlerRegistrar.init(javalin);
        // 注入MVC处理功能
        MvcConfig mvc = webConfig.getMvc();
        initMVC(javalin, mvc);
        // 自定义配置
        if (javalinCallback != null) {
            javalinCallback.accept(javalin);
        }
        javalin.start(webConfig.getHost(), webConfig.getPort());
        return javalin;
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback) {
        return init(environment, configCallback, null);
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment) {
        return init(environment, null, null);
    }

    private void initHTTP(JavalinConfig config, HttpConfig http) {
        if (http == null) {
            http = new HttpConfig();
        }
        config.autogenerateEtags = http.isAutogenerateEtags();
        config.prefer405over404 = http.isPrefer405over404();
        config.enforceSsl = http.isEnforceSsl();
        config.defaultContentType = http.getDefaultContentType();
        if (http.getMaxRequestSize() != null) {
            config.maxRequestSize = http.getMaxRequestSize().toBytes();
        }
        if (http.getAsyncRequestTimeout() != null) {
            config.asyncRequestTimeout = http.getAsyncRequestTimeout().toMillis();
        }
        if (http.getSinglePageRoot() != null) {
            for (HttpConfig.SinglePageRoot singlePageRoot : http.getSinglePageRoot()) {
                config.addSinglePageRoot(singlePageRoot.getHostedPath(), singlePageRoot.getFilePath(), singlePageRoot.getLocation());
            }
        }
        if (http.getStaticFile() != null) {
            for (HttpConfig.StaticFile staticFile : http.getStaticFile()) {
                config.addStaticFiles(staticFileConfig -> {
                    staticFileConfig.hostedPath = staticFile.getHostedPath();
                    staticFileConfig.directory = staticFile.getDirectory();
                    staticFileConfig.location = staticFile.getLocation();
                    staticFileConfig.precompress = staticFile.isPreCompress();
                    staticFileConfig.headers = staticFile.getHeaders();
                });
            }
        }
        if (http.isEnableWebjars()) {
            config.enableWebjars();
        }
        if (http.isEnableCorsForAllOrigins()) {
            config.enableCorsForAllOrigins();
        }
        if (http.getEnableCorsForOrigin() != null && !http.getEnableCorsForOrigin().isEmpty()) {
            config.enableCorsForOrigin(http.getEnableCorsForOrigin().toArray(new String[0]));
        }
    }

    private void initServer(JavalinConfig config, ServerConfig server) {
        if (server == null) {
            server = new ServerConfig();
        }
        config.contextPath = server.getContextPath();
        config.ignoreTrailingSlashes = server.isIgnoreTrailingSlashes();
        if (server.isEnableDevLogging()) {
            config.enableDevLogging();
        }
        // 注册一个请求记录器
        // config.requestLogger((ctx, executionTimeMs) -> {});

        // 自定义 Jetty Server
        ServerConfig.Threads threads = server.getThreads();
        if (threads != null) {
            config.server(() -> {
                BlockingQueue<Runnable> queue = null;
                if (threads.getMaxQueueCapacity() == 0) {
                    queue = new SynchronousQueue<>();
                } else if (threads.getMaxQueueCapacity() > 0) {
                    queue = new BlockingArrayQueue<>(threads.getMaxQueueCapacity());
                }
                int maxThreadCount = (threads.getMax() > 0) ? threads.getMax() : 250;
                int minThreadCount = (threads.getMin() > 0) ? threads.getMin() : 8;
                int threadIdleTimeout = (threads.getIdleTimeout() != null) ? (int) threads.getIdleTimeout().toMillis() : 60_000;
                QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
                queuedThreadPool.setName("JettyServerThreadPool");
                org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(queuedThreadPool);
                jettyServer = JettyUtil.getOrDefault(jettyServer);
                return jettyServer;
            });
        }

        // 自定义 Jetty SessionHandler
        // config.sessionHandler(() -> {
        //     SessionHandler sessionHandler = new SessionHandler();
        //     sessionHandler.set
        //     return sessionHandler;
        // });

        // 自定义 Jetty ServletContextHandler
        // config.configureServletContextHandler(handler -> {
        //     handler.addFilter()
        //     handler.addServlet()
        //     handler.addEventListener();
        // });
    }

    private void initWebSocket(JavalinConfig config, WebSocketConfig webSocket) {
        if (webSocket == null) {
            webSocket = new WebSocketConfig();
        }
        // 自定义 Jetty WebSocketServletFactory
        // config.wsFactoryConfig(factory -> {
        // });

        // 注册一个 WebSocket 记录器
        // config.wsLogger(ws -> {});
    }

    private void initMVC(Javalin javalin, MvcConfig mvc) {
        if (mvc == null) {
            mvc = new MvcConfig();
        }
        Handler handler = ctx -> {
        };
        for (HandlerType handlerType : mvc.getHttpMethod()) {
            javalin.addHandler(handlerType, mvc.getPath(), handler);
        }
        // TODO 注入MVC处理功能
    }

    private ObjectMapper initJackson(JacksonConfig jackson) {
        if (jackson == null) {
            jackson = new JacksonConfig();
        }
        ObjectMapper mapper = JacksonMapper.newObjectMapper();
        jackson.apply(mapper);
        return mapper;
    }

    private void initMisc(JavalinConfig config, MiscConfig misc) {
        if (misc == null) {
            misc = new MiscConfig();
        }
        config.showJavalinBanner = misc.isShowJavalinBanner();
    }
}
