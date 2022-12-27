package org.clever.web;

import io.javalin.core.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.BannerUtils;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.util.Assert;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Servlet 注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 10:48 <br/>
 */
@Slf4j
public class ServletRegistrar {
    // 保存 JavalinConfig.inner.appAttributes
    private Map<String, Object> appAttributes = Collections.emptyMap();
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
    public ServletRegistrar addServlet(Servlet servlet, String pathSpec, String name, double order) {
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
        servlets.add(new OrderServlet(new ServletAdapter(pathSpec, servlet) {
        }, pathSpec, order, name));
        return this;
    }

    /**
     * 增加 Servlet
     *
     * @param servlet  Servlet
     * @param pathSpec Servlet处理路径
     * @param name     Servlet名称
     */
    public ServletRegistrar addServlet(Servlet servlet, String pathSpec, String name) {
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

    synchronized void init(ServletContextHandler servletContextHandler, JavalinConfig.Inner inner) {
        Assert.notNull(servletContextHandler, "servletContextHandler 不能为 null");
        Assert.notNull(inner, "inner 不能为 null");
        appAttributes = inner.appAttributes;
        servlets.sort(Comparator.comparingDouble(o -> o.order));
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
        private final Servlet servlet;
        private final String pathSpec;
        private final double order;
        private final String name;
    }

    @FunctionalInterface
    public interface ServletFuc {
        void service(Context ctx) throws Exception;
    }

    public class ServletAdapter implements Servlet {
        private final String pathSpec;
        private final ServletFuc fuc;

        public ServletAdapter(String pathSpec, ServletFuc fuc) {
            Assert.notNull(fuc, "fuc 不能为 null");
            this.fuc = fuc;
            this.pathSpec = pathSpec;
        }

        @Override
        public void init(ServletConfig config) {
        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) {
            Context ctx = new Context((HttpServletRequest) req, (HttpServletResponse) res, appAttributes);
            // 参考 io.javalin.http.util.ContextUtil#update
            // String requestUri = StringUtils.removeStart(ctx.req.getRequestURI(), ctx.req.getContextPath());
            ReflectionsUtils.setFieldValue(ctx, "matchedPath", pathSpec);
            // ReflectionsUtils.setFieldValue(ctx, "pathParamMap", Collections.emptyMap());
            ReflectionsUtils.setFieldValue(ctx, "handlerType", HandlerType.Companion.fromServletRequest(ctx.req));
            ReflectionsUtils.setFieldValue(ctx, "endpointHandlerPath", pathSpec);
            try {
                fuc.service(ctx);
            } catch (Exception e) {
                throw ExceptionUtils.unchecked(e);
            }
        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {
        }
    }
}
