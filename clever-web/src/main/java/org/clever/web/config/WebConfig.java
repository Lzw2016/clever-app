package org.clever.web.config;

import lombok.Data;

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
     * web服务要绑定的主机IP，默认："127.0.0.1"
     */
    private String host = "127.0.0.1";
    /**
     * web服务端口，默认：9090
     */
    private int port = 9090;
    /**
     * HTTP 配置
     */
    private HTTP http = new HTTP();
    /**
     * Server 配置
     */
    private Server server = new Server();
    /**
     * WebSocket 配置
     */
    private WebSocket webSocketConfig = new WebSocket();
    /**
     * Misc(杂项) 配置
     */
    private Misc misc = new Misc();
}
