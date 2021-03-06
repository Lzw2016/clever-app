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
     * MVC 配置
     */
    private MVC mvc = new MVC();
    /**
     * Jackson 配置
     */
    private Jackson jackson = new Jackson();
    /**
     * Misc(杂项) 配置
     */
    private Misc misc = new Misc();
}
