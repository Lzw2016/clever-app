package org.clever.web.config;

import io.javalin.core.JavalinConfig;
import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 13:39 <br/>
 */
@Data
public class WebSocketConfig {
    // TODO 未实现

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
