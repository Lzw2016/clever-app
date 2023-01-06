package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.plugin.json.JavalinJackson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.env.Environment;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.util.Assert;
import org.clever.web.config.WebConfig;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 用于初始化和启动web服务器的工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 12:35 <br/>
 */
@Slf4j
public class WebServerBootstrap {
    public static WebServerBootstrap create(WebConfig webConfig) {
        Assert.notNull(webConfig, "参数 webConfig 不能为 null");
        return new WebServerBootstrap(webConfig);
    }

    public static WebServerBootstrap create(Environment environment) {
        WebConfig webConfig = Binder.get(environment).bind(WebConfig.PREFIX, WebConfig.class).orElseGet(WebConfig::new);
        AppContextHolder.registerBean("webConfig", webConfig, true);
        return create(webConfig);
    }

    @Getter
    private final WebConfig webConfig;
    @Getter
    private Javalin javalin;
    @Getter
    private volatile boolean initialized = false;
    @Getter
    private volatile boolean started = false;
    @Getter
    private final FilterRegistrar filterRegistrar = new FilterRegistrar();
    @Getter
    private final ServletRegistrar servletRegistrar = new ServletRegistrar();
    @Getter
    private final EventListenerRegistrar eventListenerRegistrar = new EventListenerRegistrar();
    @Getter
    private final JavalinHandlerRegistrar handlerRegistrar = new JavalinHandlerRegistrar();
    @Getter
    private final JavalinPluginRegistrar pluginRegistrar = new JavalinPluginRegistrar();
    @Getter
    private final JavalinEventListenerRegistrar javalinEventListenerRegistrar = new JavalinEventListenerRegistrar();

    public WebServerBootstrap(WebConfig webConfig) {
        this.webConfig = webConfig;
    }

    /**
     * 初始化Web服务
     */
    public Javalin init(Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        javalin = Javalin.create(config -> {
            // 初始化http相关配置
            WebConfig.HttpConfig http = webConfig.getHttp();
            Optional.of(http).orElse(new WebConfig.HttpConfig()).apply(config);
            // 初始化Server相关配置
            WebConfig.ServerConfig server = webConfig.getServer();
            Optional.of(server).orElse(new WebConfig.ServerConfig()).apply(config);
            // 初始化WebSocket相关配置
            WebConfig.WebSocketConfig webSocket = webConfig.getWebSocketConfig();
            Optional.of(webSocket).orElse(new WebConfig.WebSocketConfig()).apply(config);
            // 初始化杂项配置
            WebConfig.MiscConfig misc = webConfig.getMisc();
            Optional.of(misc).orElse(new WebConfig.MiscConfig()).apply(config);
            // 自定义 JsonMapper
            JacksonConfig jackson = webConfig.getJackson();
            ObjectMapper webServerMapper = JacksonMapper.newObjectMapper();
            Optional.of(jackson).orElse(new JacksonConfig()).apply(webServerMapper);
            config.jsonMapper(new JavalinJackson(webServerMapper));
            config.configureServletContextHandler(servletContextHandler -> {
                // 注册自定义 Filter
                filterRegistrar.init(servletContextHandler);
                // 注册自定义 Servlet
                servletRegistrar.init(servletContextHandler, config.inner);
                // 注册自定义 EventListener
                eventListenerRegistrar.init(servletContextHandler);
            });
            // 注册自定义插件
            pluginRegistrar.init(config);
            // 自定义配置
            if (configCallback != null) {
                configCallback.accept(config);
            }
        });
        // 注册自定义 Event
        javalin.events(javalinEventListenerRegistrar::init);
        // 注册自定义 Handler
        handlerRegistrar.init(javalin);
        // 自定义配置
        if (javalinCallback != null) {
            javalinCallback.accept(javalin);
        }
        return javalin;
    }

    /**
     * 初始化Web服务
     */
    public Javalin init(Consumer<JavalinConfig> configCallback) {
        return init(configCallback, null);
    }

    /**
     * 初始化Web服务
     */
    public Javalin init() {
        return init(null, null);
    }

    /**
     * 启动Web服务
     */
    public void start() {
        Assert.isTrue(initialized, "还未初始化，先执行 init 函数");
        Assert.isTrue(!started, "不能多次启动web服务");
        started = true;
        javalin.start(webConfig.getHost(), webConfig.getPort());
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin initAndStart(Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Javalin javalin = init(configCallback, javalinCallback);
        start();
        return javalin;
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin initAndStart(Consumer<JavalinConfig> configCallback) {
        return initAndStart(configCallback, null);
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin initAndStart() {
        return initAndStart(null, null);
    }
}
