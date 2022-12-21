package org.clever.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.ContentType;
import io.javalin.plugin.json.JavalinJackson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.env.Environment;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.util.Assert;
import org.clever.web.config.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
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
    protected volatile boolean initialized = false;
    @Getter
    private final JavalinPluginRegistrar pluginRegistrar = new JavalinPluginRegistrar();
    @Getter
    private final JavalinHandlerRegistrar handlerRegistrar = new JavalinHandlerRegistrar();

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback, Consumer<Javalin> javalinCallback) {
        Assert.isTrue(!initialized, "不能多次初始化");
        initialized = true;
        Assert.notNull(environment, "environment 不能为空");
        WebConfig webConfig = Binder.get(environment).bind(WebConfig.PREFIX, WebConfig.class).orElseGet(WebConfig::new);
        Javalin javalin = Javalin.create(config -> {
            // TODO 配置 Filter
            // TODO 配置 Servlet
            // TODO 配置 EventListener
            config.configureServletContextHandler(servletContextHandler -> {
                servletContextHandler.addFilter(new FilterHolder((request, response, chain) -> {
                    log.info("### Filter_之前");
                    chain.doFilter(request, response);
                    log.info("### Filter_之后");
                }), "/*", EnumSet.of(DispatcherType.REQUEST));

//                servletContextHandler.addFilter(new FilterHolder((request, response, chain) -> {
//                    log.info("### Filter_2_之前");
//                    response.setContentType(ContentType.TEXT_PLAIN.getMimeType());
//                    response.getWriter().println("提前结束请求");
//                    ((HttpServletResponse) response).setStatus(200);
//                    response.getWriter().flush();
//                    log.info("### Filter_2_之后");
//                }), "/*", EnumSet.of(DispatcherType.REQUEST));

                servletContextHandler.addFilter(new FilterHolder((request, response, chain) -> {
                    log.info("### Filter_3_之前");
                    chain.doFilter(request, response);
                    log.info("### Filter_3_之后");
                }), "/*", EnumSet.of(DispatcherType.REQUEST));

                servletContextHandler.addServlet(new ServletHolder(new Servlet() {
                    @Override
                    public void init(ServletConfig config) throws ServletException {

                    }

                    @Override
                    public ServletConfig getServletConfig() {
                        return null;
                    }

                    @Override
                    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
                        res.setContentType(ContentType.TEXT_PLAIN.getMimeType());
                        res.getWriter().println("自定义Servlet");
                        ((HttpServletResponse) res).setStatus(200);
                        res.getWriter().flush();
                    }

                    @Override
                    public String getServletInfo() {
                        return "";
                    }

                    @Override
                    public void destroy() {

                    }
                }), "/servlet/*");
            });
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
            // 注册自定义插件
            pluginRegistrar.init(config);
            // 自定义配置
            if (configCallback != null) {
                configCallback.accept(config);
            }
        });
        // 注册自定义Handler
        handlerRegistrar.init(javalin);
        // 注入MVC处理功能
        MvcConfig mvc = webConfig.getMvc();
        Optional.of(mvc).orElse(new MvcConfig()).apply(javalin);
        // 自定义配置
        if (javalinCallback != null) {
            javalinCallback.accept(javalin);
        }

//        javalin.jettyServer().getConfig()
//        javalin.javalinServlet()

        javalin.start(webConfig.getHost(), webConfig.getPort());
        return javalin;
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment, Consumer<JavalinConfig> configCallback) {
        return init(environment, configCallback, null);
    }

    /**
     * 初始化并启动Web服务
     */
    public Javalin init(Environment environment) {
        return init(environment, null, null);
    }
}
