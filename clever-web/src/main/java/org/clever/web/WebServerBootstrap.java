package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.plugin.json.JavalinJackson;
import lombok.Getter;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.env.Environment;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.util.Assert;
import org.clever.web.config.*;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 用于初始化和启动web服务器的工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 12:35 <br/>
 */
public class WebServerBootstrap {
    protected volatile boolean initialized = false;
    protected volatile boolean started = false;
    protected WebConfig webConfig;
    protected Javalin javalin;
    @Getter
    private final JavalinPluginRegistrar pluginRegistrar = new JavalinPluginRegistrar();
    @Getter
    private final JavalinHandlerRegistrar handlerRegistrar = new JavalinHandlerRegistrar();
    @Getter
    private final FilterRegistrar filterRegistrar = new FilterRegistrar();
    @Getter
    private final ServletRegistrar servletRegistrar = new ServletRegistrar();
    @Getter
    private final EventListenerRegistrar eventListenerRegistrar = new EventListenerRegistrar();

    /**
     * 初始化Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        Assert.notNull(environment, "environment 不能为空");
        webConfig = Binder.get(environment).bind(WebConfig.PREFIX, WebConfig.class).orElseGet(WebConfig::new);
        javalin = Javalin.create(config -> {
            // 初始化http相关配置
            HttpConfig http = webConfig.getHttp();
            Optional.of(http).orElse(new HttpConfig()).apply(config);
            // 初始化Server相关配置
            ServerConfig server = webConfig.getServer();
            Optional.of(server).orElse(new ServerConfig()).apply(config);
            // 初始化WebSocket相关配置
            WebSocketConfig webSocket = webConfig.getWebSocketConfig();
            Optional.of(webSocket).orElse(new WebSocketConfig()).apply(config);
            // 初始化杂项配置
            MiscConfig misc = webConfig.getMisc();
            Optional.of(misc).orElse(new MiscConfig()).apply(config);
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
        // javalin.events(eventListener -> {
        //     eventListener.serverStarted();
        // });
        // 注册自定义 Handler
        handlerRegistrar.init(javalin);
        // 注入MVC处理功能
        MvcConfig mvc = webConfig.getMvc();
        Optional.of(mvc).orElse(new MvcConfig()).apply(javalin);
        // 自定义配置
        if (javalinCallback != null) {
            javalinCallback.accept(javalin);
        }
        return javalin;
    }


    /**
     * 初始化Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback) {
        return init(environment, configCallback, null);
    }

    /**
     * 初始化Web服务
     */
    public Javalin init(Environment environment) {
        return init(environment, null, null);
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
    public Javalin initAndStart(Environment environment, Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Javalin javalin = init(environment, configCallback, javalinCallback);
        start();
        return javalin;
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin initAndStart(Environment environment, Consumer<JavalinConfig> configCallback) {
        return initAndStart(environment, configCallback, null);
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin initAndStart(Environment environment) {
        return initAndStart(environment, null, null);
    }
}
