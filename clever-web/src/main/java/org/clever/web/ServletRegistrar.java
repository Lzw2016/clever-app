package org.clever.web;

import io.javalin.config.JavalinConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;
import org.clever.core.exception.ExceptionUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Servlet 注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 10:48 <br/>
 */
@Slf4j
public class ServletRegistrar {
    private JavalinConfig javalinConfig;
    /**
     * Servlet列表
     */
    private final List<OrderServlet> servlets = new LinkedList<>();

    /**
     * 增加 Servlet
     *
     * @param servlet  Servlet
     * @param pathSpec Servlet处理路径
     * @param name     Servlet名称
     * @param order    顺序，值越小，优先级越高
     */
    public ServletRegistrar addServlet(HttpServlet servlet, String pathSpec, String name, double order) {
        Assert.notNull(servlet, "servlet 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        servlets.add(new OrderServlet(servlet, pathSpec, order, name));
        return this;
    }

    /**
     * 增加 Servlet
     *
     * @param servlet  Servlet
     * @param pathSpec Servlet处理路径
     * @param name     Servlet名称
     * @param order    顺序，值越小，优先级越高
     */
    public ServletRegistrar addServlet(ServletFuc servlet, String pathSpec, String name, double order) {
        Assert.notNull(servlet, "servlet 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        servlets.add(new OrderServlet(new ServletAdapter(servlet), pathSpec, order, name));
        return this;
    }

    /**
     * 增加 Servlet
     *
     * @param servlet  Servlet
     * @param pathSpec Servlet处理路径
     * @param name     Servlet名称
     */
    public ServletRegistrar addServlet(HttpServlet servlet, String pathSpec, String name) {
        return addServlet(servlet, pathSpec, name, 0);
    }

    /**
     * 增加 Servlet
     *
     * @param servlet  Servlet
     * @param pathSpec Servlet处理路径
     * @param name     Servlet名称
     */
    public ServletRegistrar addServlet(ServletFuc servlet, String pathSpec, String name) {
        return addServlet(servlet, pathSpec, name, 0);
    }

    synchronized void init(ServletContextHandler servletContextHandler, JavalinConfig config) {
        Assert.notNull(servletContextHandler, "servletContextHandler 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        servlets.sort(Comparator.comparingDouble(o -> o.order));
        this.javalinConfig = config;
        List<String> logs = new ArrayList<>();
        int idx = 1;
        for (OrderServlet item : servlets) {
            logs.add(String.format(
                "%2s. path=%s%s",
                idx++,
                item.pathSpec,
                StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            ));
            servletContextHandler.addServlet(new ServletHolder(item.servlet), item.pathSpec);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "自定义Servlet", logs.toArray(new String[0]));
        }
    }

    @Data
    private static class OrderServlet {
        private final HttpServlet servlet;
        private final String pathSpec;
        private final double order;
        private final String name;
    }

    @FunctionalInterface
    public interface ServletFuc {
        void service(Context ctx) throws Exception;
    }

    public class ServletAdapter extends HttpServlet {
        private final ServletFuc fuc;

        public ServletAdapter(ServletFuc fuc) {
            Assert.notNull(fuc, "fuc 不能为 null");
            this.fuc = fuc;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res) {
            Context ctx = new Context(req, res, javalinConfig);
            try {
                fuc.service(ctx);
                Context.flushResultStream(ctx);
            } catch (Exception e) {
                throw ExceptionUtils.unchecked(e);
            }
        }
    }
}
