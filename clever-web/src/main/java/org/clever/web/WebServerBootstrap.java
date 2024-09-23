package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.SizeUnit;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.util.ConcurrencyUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.AppContextHolder;
import org.clever.core.Assert;
import org.clever.core.ResourcePathUtils;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.web.config.HttpConfig;
import org.clever.web.config.WebConfig;
import org.clever.web.config.WebSocketConfig;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

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
    public static WebServerBootstrap create(String rootPath, WebConfig webConfig) {
        return new WebServerBootstrap(rootPath, webConfig);
    }

    public static WebServerBootstrap create(String rootPath, Environment environment) {
        WebConfig webConfig = Binder.get(environment).bind(WebConfig.PREFIX, WebConfig.class).orElseGet(WebConfig::new);
        AppContextHolder.registerBean("webConfig", webConfig, true);
        return create(rootPath, webConfig);
    }

    @Getter
    private final String rootPath;
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

    public WebServerBootstrap(String rootPath, WebConfig webConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(webConfig, "参数 webConfig 不能为 null");
        this.rootPath = rootPath;
        this.webConfig = webConfig;
    }

    /**
     * 初始化Web服务
     */
    public Javalin init(Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        javalin = Javalin.create(config -> {
            // 应用配置
            applyConfig(rootPath, config);
            // 配置Filter Servlet EventListener
            config.jetty.modifyServletContextHandler(servletContextHandler -> {
                // 注册自定义 Filter
                filterRegistrar.init(servletContextHandler);
                // 注册自定义 Servlet
                servletRegistrar.init(servletContextHandler, config);
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
     * 应用当前配置到 JavalinConfig
     */
    protected void applyConfig(String rootPath, JavalinConfig config) {
        Assert.notNull(config, "参数 config 不能为空");
        final JacksonConfig jackson = Optional.of(webConfig.getJackson()).orElseGet(() -> {
            webConfig.setJackson(new JacksonConfig());
            return webConfig.getJackson();
        });
        final HttpConfig http = Optional.of(webConfig.getHttp()).orElseGet(() -> {
            webConfig.setHttp(new HttpConfig());
            return webConfig.getHttp();
        });
        final WebSocketConfig websocket = Optional.of(webConfig.getWebsocket()).orElseGet(() -> {
            webConfig.setWebsocket(new WebSocketConfig());
            return webConfig.getWebsocket();
        });
        config.jetty.defaultHost = webConfig.getHost();
        config.jetty.defaultPort = webConfig.getPort();
        config.showJavalinBanner = webConfig.isShowJavalinBanner();
        config.useVirtualThreads = webConfig.isUseVirtualThreads();
        config.jetty.threadPool = ConcurrencyUtil.jettyThreadPool(
            webConfig.getThreadPoolName(),
            webConfig.getThreadPoolMin(),
            webConfig.getThreadPoolMax(),
            webConfig.isUseVirtualThreads()
        );
        config.startupWatcherEnabled = false;
        // jackson
        ObjectMapper webServerMapper = JacksonMapper.newObjectMapper();
        jackson.apply(webServerMapper);
        config.jsonMapper(new JavalinJackson(webServerMapper, webConfig.isUseVirtualThreads()));
        // http
        config.router.contextPath = http.getContextPath();
        config.router.ignoreTrailingSlashes = http.isIgnoreTrailingSlashes();
        config.router.treatMultipleSlashesAsSingleSlash = http.isTreatMultipleSlashesAsSingleSlash();
        config.router.caseInsensitiveRoutes = http.isCaseInsensitiveRoutes();
        config.http.generateEtags = http.isGenerateEtags();
        config.http.prefer405over404 = http.isPrefer405over404();
        config.http.defaultContentType = http.getDefaultContentType();
        if (http.getMaxRequestSize() != null) {
            config.http.maxRequestSize = http.getMaxRequestSize().toBytes();
        }
        if (http.getAsyncTimeout() != null) {
            config.http.asyncTimeout = http.getAsyncTimeout().toMillis();
        }
        if (http.getMultipart() != null) {
            HttpConfig.Multipart multipart = http.getMultipart();
            String location = multipart.getLocation();
            DataSize maxFileSize = multipart.getMaxFileSize();
            DataSize maxTotalRequestSize = multipart.getMaxTotalRequestSize();
            DataSize maxInMemoryFileSize = multipart.getMaxInMemoryFileSize();
            if (multipart.getLocation() != null) {
                config.jetty.multipartConfig.cacheDirectory(ResourcePathUtils.getAbsolutePath(rootPath, location));
            }
            if (maxFileSize != null) {
                config.jetty.multipartConfig.maxFileSize(maxFileSize.toBytes(), SizeUnit.BYTES);
            }
            if (maxTotalRequestSize != null) {
                config.jetty.multipartConfig.maxTotalRequestSize(maxTotalRequestSize.toBytes(), SizeUnit.BYTES);
            }
            if (maxInMemoryFileSize != null) {
                config.jetty.multipartConfig.maxInMemoryFileSize((int) maxInMemoryFileSize.toBytes(), SizeUnit.BYTES);
            }
        }
        if (http.getSinglePageRoot() != null) {
            for (HttpConfig.SinglePageRoot singlePageRoot : http.getSinglePageRoot()) {
                config.spaRoot.addFile(
                    singlePageRoot.getHostedPath(),
                    singlePageRoot.getFilePath(),
                    singlePageRoot.getLocation()
                );
            }
        }
        if (http.getStaticFile() != null) {
            for (HttpConfig.StaticFile staticFile : http.getStaticFile()) {
                config.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.hostedPath = staticFile.getHostedPath();
                    staticFileConfig.directory = staticFile.getDirectory();
                    staticFileConfig.location = staticFile.getLocation();
                    staticFileConfig.precompress = staticFile.isPreCompress();
                    staticFileConfig.headers = staticFile.getHeaders();
                });
            }
        }
        if (http.isEnableWebjars()) {
            config.staticFiles.enableWebjars();
        }
        // websocket
        // TODO 自定义 Jetty WebSocketServletFactory
        // config.wsFactoryConfig(factory -> {
        // });
        // 注册一个 WebSocket 日志记录器
        // config.wsLogger(ws -> {});
        // bundledPlugins
        if (http.isEnforceSsl()) {
            config.bundledPlugins.enableSslRedirects();
        }
        if (webConfig.isEnableDevLogging()) {
            config.bundledPlugins.enableDevLogging();
        }
        if (http.isEnableCorsForAllOrigins()) {
            config.bundledPlugins.enableCors(corsPluginConfig -> corsPluginConfig.addRule(CorsPluginConfig.CorsRule::anyHost));
        } else if (http.getEnableCorsForOrigin() != null && !http.getEnableCorsForOrigin().isEmpty()) {
            config.bundledPlugins.enableCors(corsPluginConfig -> {
                for (String host : http.getEnableCorsForOrigin()) {
                    corsPluginConfig.addRule(corsRule -> corsRule.allowHost(host));
                }
            });
        }
        if (webConfig.isEnableHttpAllowedMethodsOnRoutes()) {
            config.bundledPlugins.enableHttpAllowedMethodsOnRoutes();
        }
        if (webConfig.isEnableRedirectToLowercasePaths()) {
            config.bundledPlugins.enableRedirectToLowercasePaths();
        }
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
        javalin.start();
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
