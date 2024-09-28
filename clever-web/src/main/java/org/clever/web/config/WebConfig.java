package org.clever.web.config;

import lombok.Data;
import org.clever.core.json.jackson.JacksonConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

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
     * TODO 是否使用 javalin
     */
    private boolean useJavalin = true;
    /**
     * web服务要绑定的主机IP，默认："0.0.0.0"
     */
    private String host = "0.0.0.0";
    /**
     * web服务端口，默认：9090
     */
    private int port = 9090;
    /**
     * 是否输出 Javalin Banner 日志
     */
    private boolean showJavalinBanner = false;
    /**
     * 是否使用虚拟线程
     */
    private boolean useVirtualThreads = false;
    /**
     * 线程池的最大线程数。默认：250
     */
    private int threadPoolMax = 250;
    /**
     * 线程池的最小线程数。默认：8
     */
    private int threadPoolMin = 8;
    /**
     * 线程池名称：http-exec
     */
    private String threadPoolName = "jetty-exec";
    /**
     * 启用开发日志（用于开发的广泛调试日志），默认：false
     */
    private boolean enableDevLogging = false;
    /**
     * 启用 HttpAllowedMethodsPlugin 插件
     */
    private boolean enableHttpAllowedMethodsOnRoutes = false;
    /**
     * 启用 RedirectToLowercasePathPlugin 插件
     */
    private boolean enableRedirectToLowercasePaths = false;
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
}
