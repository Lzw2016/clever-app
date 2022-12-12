package org.clever.web.config;

import io.javalin.core.JavalinConfig;
import io.javalin.jetty.JettyUtil;
import lombok.Data;
import org.clever.util.Assert;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

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

    /**
     * 应用当前配置到 JavalinConfig
     */
    public void apply(JavalinConfig config) {
        Assert.notNull(config, "参数 config 不能为空");
        ServerConfig server = this;
        config.contextPath = server.getContextPath();
        config.ignoreTrailingSlashes = server.isIgnoreTrailingSlashes();
        if (server.isEnableDevLogging()) {
            config.enableDevLogging();
        }
        // TODO 注册一个请求记录器
        // config.requestLogger((ctx, executionTimeMs) -> {});

        // 自定义 Jetty Server
        ServerConfig.Threads threads = server.getThreads();
        if (threads != null) {
            config.server(() -> {
                BlockingQueue<Runnable> queue = null;
                if (threads.getMaxQueueCapacity() == 0) {
                    queue = new SynchronousQueue<>();
                } else if (threads.getMaxQueueCapacity() > 0) {
                    queue = new BlockingArrayQueue<>(threads.getMaxQueueCapacity());
                }
                int maxThreadCount = (threads.getMax() > 0) ? threads.getMax() : 250;
                int minThreadCount = (threads.getMin() > 0) ? threads.getMin() : 8;
                int threadIdleTimeout = (threads.getIdleTimeout() != null) ? (int) threads.getIdleTimeout().toMillis() : 60_000;
                QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
                queuedThreadPool.setName("JettyServerThreadPool");
                org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(queuedThreadPool);
                jettyServer = JettyUtil.getOrDefault(jettyServer);
                return jettyServer;
            });
        }

        // TODO 自定义 Jetty SessionHandler
        // config.sessionHandler(() -> {
        //     SessionHandler sessionHandler = new SessionHandler();
        //     sessionHandler.set
        //     return sessionHandler;
        // });

        // TODO 自定义 Jetty ServletContextHandler
        // config.configureServletContextHandler(handler -> {
        //     handler.addFilter()
        //     handler.addServlet()
        //     handler.addEventListener();
        // });
    }
}
