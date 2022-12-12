package org.clever.web.config;

import lombok.Data;

import java.time.Duration;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 13:34 <br/>
 */
@Data
public class ServerConfig {
    /**
     * web server 的 context path，默认："/"
     */
    private String contextPath = "/";
    /**
     * 将“/path”和“/path/”视为同一路径，默认：true
     */
    private boolean ignoreTrailingSlashes = true;
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
}
