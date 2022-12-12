package org.clever.web.config;

import lombok.Data;
import org.clever.core.json.jackson.JacksonConfig;

/**
 * Web 服务器配置项
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 12:35 <br/>
 */
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
     * 全局的资源根路径
     */
    private String rootPath = "";
    /**
     * HTTP 配置
     */
    private HttpConfig http = new HttpConfig();
    /**
     * Server 配置
     */
    private ServerConfig server = new ServerConfig();
    /**
     * WebSocket 配置
     */
    private WebSocketConfig webSocketConfig = new WebSocketConfig();
    /**
     * MVC 配置
     */
    private MvcConfig mvc = new MvcConfig();
    /**
     * Jackson 配置
     */
    private JacksonConfig jackson = new JacksonConfig();
    /**
     * Misc(杂项) 配置
     */
    private MiscConfig misc = new MiscConfig();
}
