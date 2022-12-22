package org.clever.web;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.util.Assert;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.util.Comparator;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

/**
 * EventListener 注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 11:42 <br/>
 */
@Slf4j
public class EventListenerRegistrar {
    /**
     * 过滤器列表
     */
    private final List<OrderEventListener> eventListeners = new LinkedList<>();

    /**
     * 增加EventListener，如：
     * <pre>
     * ServletContextListener
     * ServletContextAttributeListener
     * ServletRequestListener
     * ServletRequestAttributeListener
     * HttpSessionListener
     * 等等...
     * </pre>
     *
     * @param eventListener EventListener
     * @param name          名称
     * @param order         顺序，值越小，优先级越高
     */
    public EventListenerRegistrar addEventListener(EventListener eventListener, String name, double order) {
        Assert.notNull(eventListener, "eventListener 不能为 null");
        eventListeners.add(new OrderEventListener(eventListener, order, name));
        return this;
    }

    /**
     * 增加EventListener，如：
     * <pre>
     * ServletContextListener
     * ServletContextAttributeListener
     * ServletRequestListener
     * ServletRequestAttributeListener
     * HttpSessionListener
     * 等等...
     * </pre>
     *
     * @param eventListener EventListener
     * @param name          名称
     */
    public EventListenerRegistrar addEventListener(EventListener eventListener, String name) {
        return addEventListener(eventListener, name, 0);
    }

    synchronized void init(ServletContextHandler servletContextHandler) {
        Assert.notNull(servletContextHandler, "servletContextHandler 不能为 null");
        eventListeners.sort(Comparator.comparingDouble(o -> o.order));
        int idx = 1;
        for (OrderEventListener item : eventListeners) {
            log.info(
                    "# EventListener {}{}",
                    String.format("%-2s", idx++),
                    StringUtils.isNoneBlank(item.name) ? String.format(" | %s", item.name) : ""
            );
            servletContextHandler.addEventListener(item.eventListener);
        }
    }

    @Data
    private static class OrderEventListener {
        private final EventListener eventListener;
        private final double order;
        private final String name;
    }
}
