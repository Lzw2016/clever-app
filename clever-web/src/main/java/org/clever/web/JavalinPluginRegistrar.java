package org.clever.web;

import io.javalin.config.JavalinConfig;
import io.javalin.plugin.Plugin;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Javalin 插件的注册器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/26 17:22 <br/>
 */
@Slf4j
public class JavalinPluginRegistrar {
    /**
     * 插件列表
     */
    private final List<OrderPlugin<?>> plugins = new LinkedList<>();

    /**
     * 增加插件
     *
     * @param plugin 插件
     * @param name   插件名称
     * @param order  顺序，值越小，优先级越高
     */
    public JavalinPluginRegistrar addPlugin(Plugin<?> plugin, String name, double order) {
        Assert.notNull(plugin, "plugin 不能为 null");
        plugins.add(new OrderPlugin<>(plugin, order, name));
        return this;
    }

    /**
     * 增加插件
     *
     * @param plugin 插件
     * @param name   插件名称
     */
    public JavalinPluginRegistrar addPlugin(Plugin<?> plugin, String name) {
        return addPlugin(plugin, name, 0);
    }

    synchronized void init(JavalinConfig config) {
        Assert.notNull(config, "config 不能为 null");
        plugins.sort(Comparator.comparingDouble(o -> o.order));
        List<String> logs = new ArrayList<>();
        int idx = 1;
        for (OrderPlugin<?> item : plugins) {
            logs.add(String.format(
                "%2s. %s",
                idx++,
                StringUtils.isNoneBlank(item.name) ? item.name.trim() : "JavalinPlugin"
            ));
            config.registerPlugin(item.plugin);
        }
        if (!logs.isEmpty()) {
            BannerUtils.printConfig(log, "Javalin Plugin", logs.toArray(new String[0]));
        }
    }

    @Data
    private static class OrderPlugin<T> {
        private final Plugin<T> plugin;
        private final double order;
        private final String name;
    }
}
