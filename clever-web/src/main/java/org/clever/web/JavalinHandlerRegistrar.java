package org.clever.web;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.websocket.WsConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Javalin 拦截器的注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/26 21:12 <br/>
 */
@Slf4j
public class JavalinHandlerRegistrar {
    /**
     * before 拦截器 {@code Map<path, List<Handler>}
     */
    private final List<OrderItem<Handler>> beforeHandler = new LinkedList<>();
    /**
     * after 拦截器 {@code Map<path, List<Handler>}
     */
    private final List<OrderItem<Handler>> afterHandler = new LinkedList<>();
    /**
     * WebSocket before 拦截器 {@code Map<path, List<Handler>}
     */
    private final List<OrderItem<Consumer<WsConfig>>> wsBeforeHandler = new LinkedList<>();
    /**
     * WebSocket after 拦截器 {@code Map<path, List<Handler>}
     */
    private final List<OrderItem<Consumer<WsConfig>>> wsAfterHandler = new LinkedList<>();

    /**
     * 增加 Before 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param name    拦截器名称
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addBeforeHandler(String path, double order, String name, Handler handler) {
        Assert.notNull(handler, "handler 不能为 null");
        Assert.isNotBlank(path, "path 不能为空");
        beforeHandler.add(new OrderItem<>(handler, path, order, name));
        return this;
    }

    /**
     * 增加 Before 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addBeforeHandler(String path, double order, Handler handler) {
        return addBeforeHandler(path, order, null, handler);
    }

    /**
     * 增加 Before 拦截器
     *
     * @param path    拦截路径
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addBeforeHandler(String path, Handler handler) {
        return addBeforeHandler(path, 0, null, handler);
    }

    /**
     * 增加 After 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param name    拦截器名称
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addAfterHandler(String path, double order, String name, Handler handler) {
        Assert.notNull(handler, "handler 不能为 null");
        Assert.isNotBlank(path, "path 不能为空");
        afterHandler.add(new OrderItem<>(handler, path, order, name));
        return this;
    }

    /**
     * 增加 After 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addAfterHandler(String path, double order, Handler handler) {
        return addAfterHandler(path, order, null, handler);
    }

    /**
     * 增加 After 拦截器
     *
     * @param path    拦截路径
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addAfterHandler(String path, Handler handler) {
        return addAfterHandler(path, 0, null, handler);
    }

    /**
     * 增加 WebSocket  Before 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param name    拦截器名称
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsBeforeHandler(String path, double order, String name, Consumer<WsConfig> handler) {
        Assert.notNull(handler, "handler 不能为 null");
        Assert.isNotBlank(path, "path 不能为空");
        wsBeforeHandler.add(new OrderItem<>(handler, path, order, name));
        return this;
    }

    /**
     * 增加 WebSocket  Before 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsBeforeHandler(String path, double order, Consumer<WsConfig> handler) {
        return addWsBeforeHandler(path, order, null, handler);
    }

    /**
     * 增加 WebSocket  Before 拦截器
     *
     * @param path    拦截路径
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsBeforeHandler(String path, Consumer<WsConfig> handler) {
        return addWsBeforeHandler(path, 0, null, handler);
    }

    /**
     * 增加 WebSocket After 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param name    拦截器名称
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsAfterHandler(String path, double order, String name, Consumer<WsConfig> handler) {
        Assert.notNull(handler, "handler 不能为 null");
        Assert.isNotBlank(path, "path 不能为空");
        wsBeforeHandler.add(new OrderItem<>(handler, path, order, name));
        return this;
    }

    /**
     * 增加 WebSocket After 拦截器
     *
     * @param path    拦截路径
     * @param order   顺序，值越小，优先级越高
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsAfterHandler(String path, double order, Consumer<WsConfig> handler) {
        return addWsBeforeHandler(path, order, null, handler);
    }

    /**
     * 增加 WebSocket After 拦截器
     *
     * @param path    拦截路径
     * @param handler 拦截器
     */
    public synchronized JavalinHandlerRegistrar addWsAfterHandler(String path, Consumer<WsConfig> handler) {
        return addWsBeforeHandler(path, 0, null, handler);
    }

    /**
     * 按照定义顺序注册所有 Handler
     */
    synchronized void init(Javalin javalin) {
        Assert.notNull(javalin, "javalin 不能为 null");
        List<String> logs = new ArrayList<>();
        beforeHandler.sort(Comparator.comparingDouble(o -> o.order));
        int idx = 1;
        for (OrderItem<Handler> handler : beforeHandler) {
            logs.add(String.format(
                "%2s. path=%s%s",
                idx++,
                handler.path,
                StringUtils.isNoneBlank(handler.name) ? String.format(" | %s", handler.name) : ""
            ));
            javalin.before(handler.path, handler.item);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin BeforeHandler", logs.toArray(new String[0]));
        }
        afterHandler.sort(Comparator.comparingDouble(o -> o.order));
        logs.clear();
        idx = 1;
        for (OrderItem<Handler> handler : afterHandler) {
            logs.add(String.format(
                "%2s. path=%s%s",
                idx++,
                handler.path,
                StringUtils.isNoneBlank(handler.name) ? String.format(" | %s", handler.name) : ""
            ));
            javalin.after(handler.path, handler.item);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin AfterHandler", logs.toArray(new String[0]));
        }
        wsBeforeHandler.sort(Comparator.comparingDouble(o -> o.order));
        logs.clear();
        idx = 1;
        for (OrderItem<Consumer<WsConfig>> handler : wsBeforeHandler) {
            logs.add(String.format(
                "%2s. path=%s%s",
                idx++,
                handler.path,
                StringUtils.isNoneBlank(handler.name) ? String.format(" | %s", handler.name) : ""
            ));
            javalin.wsBefore(handler.path, handler.item);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin WebSocket BeforeHandler", logs.toArray(new String[0]));
        }
        wsAfterHandler.sort(Comparator.comparingDouble(o -> o.order));
        logs.clear();
        idx = 1;
        for (OrderItem<Consumer<WsConfig>> handler : wsAfterHandler) {
            logs.add(String.format(
                "%2s. path=%s%s",
                idx++,
                handler.path,
                StringUtils.isNoneBlank(handler.name) ? String.format(" | %s", handler.name) : ""
            ));
            javalin.wsAfter(handler.path, handler.item);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin WebSocket AfterHandler", logs.toArray(new String[0]));
        }
    }

    @Data
    private static class OrderItem<T> {
        private final T item;
        private final String path;
        private final double order;
        private final String name;
    }
}
