package org.clever.web.config;

import io.javalin.config.JavalinConfig;
import io.javalin.http.ContentType;
import io.javalin.http.Header;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.bundled.CorsPluginConfig;
import jakarta.servlet.MultipartConfigElement;
import lombok.Data;
import org.clever.core.Assert;
import org.clever.core.json.jackson.JacksonConfig;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Web 服务器配置项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 12:35 <br/>
 */
@ConfigurationProperties(prefix = WebConfig.PREFIX)
@Data
public class WebConfig {
    public static final String PREFIX = "web";
    /**
     * web服务要绑定的主机IP，默认："0.0.0.0"
     */
    private String host = "0.0.0.0";
    /**
     * web服务端口，默认：9090
     */
    private int port = 9090;
    /**
     * Server 配置
     */
    @NestedConfigurationProperty
    private ServerConfig server = new ServerConfig();
    /**
     * Jackson 配置
     */
    @NestedConfigurationProperty
    private JacksonConfig jackson = new JacksonConfig();
    /**
     * HTTP 配置
     */
    @NestedConfigurationProperty
    private HttpConfig http = new HttpConfig();
    /**
     * WebSocket 配置
     */
    @NestedConfigurationProperty
    private WebSocketConfig websocket = new WebSocketConfig();
    /**
     * Misc(杂项) 配置
     */
    @NestedConfigurationProperty
    private MiscConfig misc = new MiscConfig();

    @Data
    public static class ServerConfig {
        /**
         * 启用开发日志（用于开发的广泛调试日志），默认：false
         */
        private boolean enableDevLogging = false;
        /**
         * Server 线程池配置，默认：null
         */
        private Threads threads;

        @Data
        public static class Threads {
            /**
             * 最大线程数。默认：250
             */
            private int max = 250;
            /**
             * 最小线程数。默认：8
             */
            private int min = 8;
            /**
             * 线程池的后备队列的最大容量。默认：null
             * <p>根据线程配置计算默认值。
             */
            private Integer maxQueueCapacity;
            /**
             * 最大线程空闲时间。默认：60s
             */
            private Duration idleTimeout = Duration.ofSeconds(60);
        }

        /**
         * 应用当前配置到 JavalinConfig
         */
        public void apply(JavalinConfig config) {
            Assert.notNull(config, "参数 config 不能为空");
            ServerConfig server = this;
            if (server.isEnableDevLogging()) {
                config.bundledPlugins.enableDevLogging();
            }
            // 自定义 Jetty Server
            ServerConfig.Threads threads = server.getThreads();
            if (threads != null) {
                BlockingQueue<Runnable> queue = null;
                if (Objects.equals(threads.getMaxQueueCapacity(), 0)) {
                    queue = new SynchronousQueue<>();
                } else if (threads.getMaxQueueCapacity() != null && threads.getMaxQueueCapacity() > 0) {
                    queue = new BlockingArrayQueue<>(threads.getMaxQueueCapacity());
                }
                int maxThreadCount = (threads.getMax() > 0) ? threads.getMax() : 250;
                int minThreadCount = (threads.getMin() > 0) ? threads.getMin() : 8;
                int threadIdleTimeout = (threads.getIdleTimeout() != null) ? (int) threads.getIdleTimeout().toMillis() : 60_000;
                QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
                queuedThreadPool.setName("JettyServerThreadPool");
                config.jetty.threadPool = queuedThreadPool;
            }
        }
    }

    @Data
    public static class HttpConfig {
        /**
         * web server 的 context path，默认："/"
         */
        private String contextPath = "/";
        /**
         * 将“/path”和“/path/”视为同一路径，默认：true
         */
        private boolean ignoreTrailingSlashes = true;
        /**
         * 如果为true，则将“/path//sub_path”和“/path/sub_path”视为相同的路径(默认值：false)
         */
        private boolean treatMultipleSlashesAsSingleSlash = false;
        /**
         * 如果为true，则将“/PATH”和“/path”视为相同的路径(默认值：false)
         */
        private boolean caseInsensitiveRoutes = false;
        /**
         * 为响应生成 etag，默认：false (ETag是HTTP协议提供的一种Web缓存验证机制，并且允许客户端进行缓存协商。这就使得缓存变得更加高效，而且节省带宽)
         */
        private boolean generateEtags = false;
        /**
         * 如果路径映射到不同的 HTTP 方法，则返回 405 而不是 404，默认：true
         */
        private boolean prefer405over404 = true;
        /**
         * 将所有 http 请求重定向到 https，默认：false
         */
        private boolean enforceSsl = false;
        /**
         * 默认响应内容类型，默认："application/json"
         */
        private String defaultContentType = ContentType.JSON;
        /**
         * 默认响应字符编码，默认："utf-8"
         */
        private String defaultCharacterEncoding = "utf-8";
        /**
         * 最大请求大小，默认：1MB (要么增加这个，要么使用 InputStream 来处理大的请求)
         */
        private DataSize maxRequestSize = DataSize.ofMegabytes(1);
        /**
         * 异步请求超时时间，默认：0s (0表示不超时)
         */
        private Duration asyncTimeout = Duration.ofSeconds(0);
        /**
         * 用于配置 {@link MultipartConfigElement} 的属性
         */
        private Multipart multipart = new Multipart();
        /**
         * 花哨的 404 处理程序，在 "/path" 上返回 404 的指定文件(常用于单页面应用)
         */
        private List<SinglePageRoot> singlePageRoot = new ArrayList<>();
        /**
         * 静态资源映射
         */
        private List<StaticFile> staticFile = new ArrayList<>();
        /**
         * 通过 webjars 添加静态文件，默认：false
         */
        private boolean enableWebjars = false;
        /**
         * 为所有源启用 CORS，默认：false
         */
        private boolean enableCorsForAllOrigins = false;
        /**
         * 为指定源启用 CORS，默认：[]
         */
        private Set<String> enableCorsForOrigin = new HashSet<>();

        @Data
        public static class Multipart {
            /**
             * 是否启用分段上传支持
             */
            private boolean enabled = true;
            /**
             * 上传文件的临时位置，默认值：“System.getProperty("java.io.tmpdir")”
             */
            private String location = System.getProperty("java.io.tmpdir");
            /**
             * 最大文件大小，默认值：“10MB”
             */
            private DataSize maxFileSize = DataSize.ofMegabytes(10);
            /**
             * 最大请求大小，默认值：“50MB”
             */
            private DataSize maxRequestSize = DataSize.ofMegabytes(50);
            /**
             * 将文件写入磁盘的阈值，，默认值：“4KB”
             */
            private DataSize fileSizeThreshold = DataSize.ofKilobytes(4);
            /**
             * 是否在文件或参数访问时延迟解析多部分请求
             */
            private boolean resolveLazily = false;
        }

        @Data
        public static class SinglePageRoot {
            /**
             * 服务端路径
             */
            private String hostedPath;
            /**
             * 文件路径
             */
            private String filePath;
            /**
             * 文件位置，默认：“CLASSPATH”
             */
            private Location location = Location.CLASSPATH;
        }

        @Data
        public static class StaticFile {
            /**
             * 服务端路径
             */
            private String hostedPath;
            /**
             * 文件路径
             */
            private String directory;
            /**
             * 文件位置，默认：“CLASSPATH”
             */
            private Location location = Location.CLASSPATH;
            /**
             * 预压缩，默认：false
             */
            private boolean preCompress = false;
            /**
             * 自定义响应头，默认：["Cache-Control": "max-age=0"]
             */
            private Map<String, String> headers = new HashMap<>(1) {{
                put(Header.CACHE_CONTROL, "max-age=0");
            }};
        }

        /**
         * 应用当前配置到 JavalinConfig
         */
        public void apply(JavalinConfig config) {
            Assert.notNull(config, "参数 config 不能为空");
            HttpConfig http = this;
            config.router.contextPath = http.getContextPath();
            config.router.ignoreTrailingSlashes = http.isIgnoreTrailingSlashes();
            config.router.caseInsensitiveRoutes = http.isCaseInsensitiveRoutes();
            config.http.generateEtags = http.isGenerateEtags();
            config.http.prefer405over404 = http.isPrefer405over404();
            if (http.isEnforceSsl()) {
                config.bundledPlugins.enableSslRedirects();
            }
            config.http.defaultContentType = http.getDefaultContentType();
            if (http.getMaxRequestSize() != null) {
                config.http.maxRequestSize = http.getMaxRequestSize().toBytes();
            }
            if (http.getAsyncTimeout() != null) {
                config.http.asyncTimeout = http.getAsyncTimeout().toMillis();
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
            if (http.isEnableCorsForAllOrigins()) {
                config.bundledPlugins.enableCors(corsPluginConfig -> corsPluginConfig.addRule(CorsPluginConfig.CorsRule::anyHost));
            }
            if (http.getEnableCorsForOrigin() != null && !http.getEnableCorsForOrigin().isEmpty()) {
                config.bundledPlugins.enableCors(corsPluginConfig -> {
                    for (String host : http.getEnableCorsForOrigin()) {
                        corsPluginConfig.addRule(corsRule -> corsRule.allowHost(host));
                    }
                });
            }
        }
    }

    @Data
    public static class WebSocketConfig {
        // TODO WebSocket未实现

        /**
         * 应用当前配置到 JavalinConfig
         */
        public void apply(JavalinConfig config) {
            // 自定义 Jetty WebSocketServletFactory
            // config.wsFactoryConfig(factory -> {
            // });

            // 注册一个 WebSocket 记录器
            // config.wsLogger(ws -> {});
        }
    }

    @Data
    public static class MiscConfig {
        /**
         * 是否输出 Javalin Banner 日志
         */
        private boolean showJavalinBanner = false;

        /**
         * 应用当前配置到 JavalinConfig
         */
        public void apply(JavalinConfig config) {
            Assert.notNull(config, "参数 config 不能为空");
            MiscConfig misc = this;
            config.showJavalinBanner = misc.isShowJavalinBanner();
        }
    }

    /**
     * 应用当前配置到 JavalinConfig
     */
    public void apply(JavalinConfig config) {
        Assert.notNull(config, "参数 config 不能为空");
        config.jetty.defaultHost = this.host;
        config.jetty.defaultPort = this.port;
        if (this.server != null) {
            this.server.apply(config);
        }
        if (this.http != null) {
            this.http.apply(config);
        }
        if (this.websocket != null) {
            this.websocket.apply(config);
        }
//        if (this.jackson != null) {
//            this.jackson.apply(config);
//        }
        if (this.misc != null) {
            this.misc.apply(config);
        }
    }
}
