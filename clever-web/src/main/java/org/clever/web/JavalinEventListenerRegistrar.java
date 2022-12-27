package org.clever.web;

import io.javalin.core.event.EventListener;
import io.javalin.core.event.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.BannerUtils;
import org.clever.util.Assert;

import java.util.*;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 15:40 <br/>
 */
@Slf4j
public class JavalinEventListenerRegistrar {
    private final LinkedHashMap<JavalinEvent, LinkedList<OrderEventHandler>> eventHandlerMap = new LinkedHashMap<>();
    private final List<OrderHandlerMetaInfo> handlerMetaInfos = new LinkedList<>();
    private final List<OrderWsHandlerMetaInfo> wsHandlerMetaInfos = new LinkedList<>();

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addListener(JavalinEvent event, EventHandler eventHandler, String name, double order) {
        Assert.notNull(event, "event 不能为 null");
        Assert.notNull(eventHandler, "eventHandler 不能为 null");
        eventHandlerMap.computeIfAbsent(event, e -> new LinkedList<>())
                .add(new OrderEventHandler(eventHandler, order, name));
        return this;
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStartingListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_STARTING, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStartingListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_STARTING, eventHandler, name, 0);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStartedListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_STARTED, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStartedListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_STARTED, eventHandler, name, 0);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStartFailedListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_START_FAILED, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStartFailedListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_START_FAILED, eventHandler, name, 0);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStopFailedListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_STOP_FAILED, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStopFailedListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_STOP_FAILED, eventHandler, name, 0);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStoppingListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_STOPPING, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStoppingListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_STOPPING, eventHandler, name, 0);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     * @param order        顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addServerStoppedListener(EventHandler eventHandler, String name, double order) {
        return addListener(JavalinEvent.SERVER_STOPPED, eventHandler, name, order);
    }

    /**
     * @param eventHandler EventHandler
     * @param name         名称
     */
    public JavalinEventListenerRegistrar addServerStoppedListener(EventHandler eventHandler, String name) {
        return addListener(JavalinEvent.SERVER_STOPPED, eventHandler, name, 0);
    }

    /**
     * @param callback 事件回调
     * @param name     名称
     * @param order    顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addHandlerAddedListener(Consumer<HandlerMetaInfo> callback, String name, double order) {
        Assert.notNull(callback, "callback 不能为 null");
        handlerMetaInfos.add(new OrderHandlerMetaInfo(callback, order, name));
        return this;
    }

    /**
     * @param callback 事件回调
     * @param name     名称
     */
    public JavalinEventListenerRegistrar addHandlerAddedListener(Consumer<HandlerMetaInfo> callback, String name) {
        return addHandlerAddedListener(callback, name, 0);
    }

    /**
     * @param callback 事件回调
     * @param name     名称
     * @param order    顺序，值越小，优先级越高
     */
    public JavalinEventListenerRegistrar addWsHandlerAddedListener(Consumer<WsHandlerMetaInfo> callback, String name, double order) {
        Assert.notNull(callback, "callback 不能为 null");
        wsHandlerMetaInfos.add(new OrderWsHandlerMetaInfo(callback, order, name));
        return this;
    }

    /**
     * @param callback 事件回调
     * @param name     名称
     */
    public JavalinEventListenerRegistrar addWsHandlerAddedListener(Consumer<WsHandlerMetaInfo> callback, String name) {
        return addWsHandlerAddedListener(callback, name, 0);
    }

    synchronized void init(EventListener eventListener) {
        Assert.notNull(eventListener, "eventListener 不能为 null");
        List<String> logs = new ArrayList<>();
        for (Map.Entry<JavalinEvent, LinkedList<OrderEventHandler>> entry : eventHandlerMap.entrySet()) {
            JavalinEvent event = entry.getKey();
            LinkedList<OrderEventHandler> items = entry.getValue();
            items.sort(Comparator.comparingDouble(o -> o.order));
            int idx = 1;
            for (OrderEventHandler item : items) {
                logs.add(String.format(
                        "%2s. %s%s",
                        idx++,
                        event,
                        StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
                ));
                switch (event) {
                    case SERVER_STARTING:
                        eventListener.serverStarting(item.eventHandler);
                        break;
                    case SERVER_STARTED:
                        eventListener.serverStarted(item.eventHandler);
                        break;
                    case SERVER_START_FAILED:
                        eventListener.serverStartFailed(item.eventHandler);
                        break;
                    case SERVER_STOP_FAILED:
                        eventListener.serverStopFailed(item.eventHandler);
                        break;
                    case SERVER_STOPPING:
                        eventListener.serverStopping(item.eventHandler);
                        break;
                    case SERVER_STOPPED:
                        eventListener.serverStopped(item.eventHandler);
                        break;
                }
            }
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "JavalinEventListener监听", logs.toArray(new String[0]));
        }
        handlerMetaInfos.sort(Comparator.comparingDouble(o -> o.order));
        logs.clear();
        int idx = 1;
        for (OrderHandlerMetaInfo item : handlerMetaInfos) {
            logs.add(String.format(
                    "%2s. HandlerAdded%s",
                    idx++,
                    StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            ));
            eventListener.handlerAdded(item.callback);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin新增Http Handler监听", logs.toArray(new String[0]));
        }
        wsHandlerMetaInfos.sort(Comparator.comparingDouble(o -> o.order));
        logs.clear();
        idx = 1;
        for (OrderWsHandlerMetaInfo item : wsHandlerMetaInfos) {
            logs.add(String.format(
                    "%2s. WsHandlerAdded%s",
                    idx++,
                    StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            ));
            eventListener.wsHandlerAdded(item.callback);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin新增Websocket Handler监听", logs.toArray(new String[0]));
        }
    }

    @Data
    private static class OrderEventHandler {
        private final EventHandler eventHandler;
        private final double order;
        private final String name;
    }

    @Data
    private static class OrderHandlerMetaInfo {
        private final Consumer<HandlerMetaInfo> callback;
        private final double order;
        private final String name;
    }

    @Data
    private static class OrderWsHandlerMetaInfo {
        private final Consumer<WsHandlerMetaInfo> callback;
        private final double order;
        private final String name;
    }
}
