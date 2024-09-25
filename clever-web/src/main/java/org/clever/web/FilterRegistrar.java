package org.clever.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.util.*;

/**
 * Filter 注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 10:08 <br/>
 */
@Slf4j
public class FilterRegistrar {
    /**
     * 过滤器列表
     */
    private final List<OrderFilter> filters = new LinkedList<>();

    /**
     * 增加过滤器
     *
     * @param filter     过滤器
     * @param pathSpec   过滤器拦截路径
     * @param dispatches 过滤器拦截 DispatcherType
     * @param name       过滤器名称
     * @param order      顺序，值越小，优先级越高
     */
    public FilterRegistrar addFilter(HttpFilter filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name, double order) {
        Assert.notNull(filter, "filter 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        Assert.notEmpty(dispatches, "dispatches 不能为空");
        filters.add(new OrderFilter(filter, pathSpec, dispatches, order, name));
        return this;
    }

    /**
     * 增加过滤器
     *
     * @param filter     过滤器
     * @param pathSpec   过滤器拦截路径
     * @param dispatches 过滤器拦截 DispatcherType
     * @param name       过滤器名称
     * @param order      顺序，值越小，优先级越高
     */
    public FilterRegistrar addFilter(FilterFuc filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name, double order) {
        Assert.notNull(filter, "filter 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        Assert.notEmpty(dispatches, "dispatches 不能为空");
        filters.add(new OrderFilter(new FilterAdapter(filter), pathSpec, dispatches, order, name));
        return this;
    }

    /**
     * 增加过滤器
     *
     * @param filter     过滤器
     * @param pathSpec   过滤器拦截路径
     * @param dispatches 过滤器拦截 DispatcherType
     * @param name       过滤器名称
     */
    public FilterRegistrar addFilter(HttpFilter filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name) {
        return addFilter(filter, pathSpec, dispatches, name, 0);
    }

    /**
     * 增加过滤器
     *
     * @param filter     过滤器
     * @param pathSpec   过滤器拦截路径
     * @param dispatches 过滤器拦截 DispatcherType
     * @param name       过滤器名称
     */
    public FilterRegistrar addFilter(FilterFuc filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name) {
        return addFilter(filter, pathSpec, dispatches, name, 0);
    }

    /**
     * 增加过滤器
     *
     * @param filter   过滤器
     * @param pathSpec 过滤器拦截路径
     * @param name     过滤器名称
     * @param order    顺序，值越小，优先级越高
     */
    public FilterRegistrar addFilter(HttpFilter filter, String pathSpec, String name, double order) {
        Assert.notNull(filter, "filter 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        filters.add(new OrderFilter(filter, pathSpec, order, name));
        return this;
    }

    /**
     * 增加过滤器
     *
     * @param filter   过滤器
     * @param pathSpec 过滤器拦截路径
     * @param name     过滤器名称
     * @param order    顺序，值越小，优先级越高
     */
    public FilterRegistrar addFilter(FilterFuc filter, String pathSpec, String name, double order) {
        Assert.notNull(filter, "filter 不能为 null");
        Assert.isNotBlank(pathSpec, "pathSpec 不能为空");
        filters.add(new OrderFilter(new FilterAdapter(filter), pathSpec, order, name));
        return this;
    }

    /**
     * 增加过滤器
     *
     * @param filter   过滤器
     * @param pathSpec 过滤器拦截路径
     * @param name     过滤器名称
     */
    public FilterRegistrar addFilter(HttpFilter filter, String pathSpec, String name) {
        return addFilter(filter, pathSpec, name, 0);
    }

    /**
     * 增加过滤器
     *
     * @param filter   过滤器
     * @param pathSpec 过滤器拦截路径
     * @param name     过滤器名称
     */
    public FilterRegistrar addFilter(FilterFuc filter, String pathSpec, String name) {
        return addFilter(filter, pathSpec, name, 0);
    }

    synchronized void init(ServletContextHandler servletContextHandler) {
        Assert.notNull(servletContextHandler, "servletContextHandler 不能为 null");
        filters.sort(Comparator.comparingDouble(o -> o.order));
        List<String> logs = new ArrayList<>();
        int idx = 1;
        int pathSpecMaxLength = filters.stream().map(item -> StringUtils.length(item.pathSpec)).max(Integer::compare).orElse(0) + 2;
        int dispatchesMaxLength = filters.stream().map(item -> StringUtils.length(item.dispatches.toString())).max(Integer::compare).orElse(0) + 2;
        for (OrderFilter item : filters) {
            logs.add(String.format(
                "%2s. path=%s | dispatches=%s%s",
                idx++,
                StringUtils.rightPad(item.pathSpec, pathSpecMaxLength),
                StringUtils.rightPad(item.dispatches.toString(), dispatchesMaxLength),
                StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            ));
            servletContextHandler.addFilter(new FilterHolder(item.filter), item.pathSpec, item.dispatches);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "自定义Filter", logs.toArray(new String[0]));
        }
    }

    @Data
    private static class OrderFilter {
        private final HttpFilter filter;
        private final String pathSpec;
        private final EnumSet<DispatcherType> dispatches;
        private final double order;
        private final String name;

        public OrderFilter(HttpFilter filter, String pathSpec, EnumSet<DispatcherType> dispatches, double order, String name) {
            this.filter = filter;
            this.pathSpec = pathSpec;
            this.dispatches = dispatches;
            this.order = order;
            this.name = name;
        }

        public OrderFilter(HttpFilter filter, String pathSpec, double order, String name) {
            this(filter, pathSpec, EnumSet.allOf(DispatcherType.class), order, name);
        }
    }

    @AllArgsConstructor
    public static class Context {
        public final HttpServletRequest req;
        public final HttpServletResponse res;
        public final FilterChain chain;

        /**
         * 执行下一个过滤器
         */
        public void next() throws ServletException, IOException {
            chain.doFilter(req, res);
        }
    }

    @FunctionalInterface
    public interface FilterFuc {
        void doFilter(Context ctx) throws IOException, ServletException;
    }

    public static class FilterAdapter extends HttpFilter {
        private final FilterFuc fuc;

        public FilterAdapter(FilterFuc fuc) {
            Assert.notNull(fuc, "fuc 不能为 null");
            this.fuc = fuc;
        }

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            fuc.doFilter(new Context(req, res, chain));
        }
    }
}
