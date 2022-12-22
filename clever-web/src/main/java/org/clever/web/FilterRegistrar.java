package org.clever.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.exception.ExceptionUtils;
import org.clever.util.Assert;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

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
    public FilterRegistrar addFilter(Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name, double order) {
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
    public FilterRegistrar addFilter(Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches, String name) {
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
    public FilterRegistrar addFilter(Filter filter, String pathSpec, String name, double order) {
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
    public FilterRegistrar addFilter(Filter filter, String pathSpec, String name) {
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
        int idx = 1;
        for (OrderFilter item : filters) {
            log.info(
                    "# Filter {} | path={} | dispatches={}{}",
                    String.format("%-2s", idx++),
                    item.pathSpec,
                    item.dispatches,
                    StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            );
            servletContextHandler.addFilter(new FilterHolder(item.filter), item.pathSpec, item.dispatches);
        }
    }

    @Data
    private static class OrderFilter {
        private final Filter filter;
        private final String pathSpec;
        private final EnumSet<DispatcherType> dispatches;
        private final double order;
        private final String name;

        public OrderFilter(Filter filter, String pathSpec, EnumSet<DispatcherType> dispatches, double order, String name) {
            this.filter = filter;
            this.pathSpec = pathSpec;
            this.dispatches = dispatches;
            this.order = order;
            this.name = name;
        }

        public OrderFilter(Filter filter, String pathSpec, double order, String name) {
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
        public void next() {
            try {
                chain.doFilter(req, res);
            } catch (Exception e) {
                throw ExceptionUtils.unchecked(e);
            }
        }
    }

    @FunctionalInterface
    public interface FilterFuc {
        void doFilter(Context ctx) throws Exception;
    }

    public static class FilterAdapter implements Filter {
        private final FilterFuc fuc;

        public FilterAdapter(FilterFuc fuc) {
            Assert.notNull(fuc, "fuc 不能为 null");
            this.fuc = fuc;
        }

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
            try {
                fuc.doFilter(new Context((HttpServletRequest) request, (HttpServletResponse) response, chain));
            } catch (Exception e) {
                throw ExceptionUtils.unchecked(e);
            }
        }

        @Override
        public void destroy() {
        }
    }
}
