package org.clever.web.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.StaticResourceConfig;
import org.clever.web.http.StaticResourceHandler;
import org.clever.web.utils.CheckResourceModified;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 22:00 <br/>
 */
@Slf4j
public class StaticResourceFilter implements FilterRegistrar.FilterFuc {
    public static StaticResourceFilter create(String rootPath, StaticResourceConfig staticResourceConfig) {
        Assert.notNull(staticResourceConfig, "参数 staticResourceConfig 不能为 null");
        return new StaticResourceFilter(staticResourceConfig.isEnable(), createHandler(rootPath, staticResourceConfig.getMappings()));
    }

    public static StaticResourceFilter create(String rootPath, Environment environment) {
        StaticResourceConfig staticResourceConfig = Binder.get(environment)
                .bind(StaticResourceConfig.PREFIX, StaticResourceConfig.class)
                .orElseGet(StaticResourceConfig::new);
        List<StaticResourceHandler> handlers = createHandler(rootPath, staticResourceConfig.getMappings());
        int maxLength = handlers.stream().map(StaticResourceHandler::getHostedPath)
                .max(Comparator.comparingInt(String::length))
                .orElse("").length();
        List<String> props = new ArrayList<>();
        props.add(StringUtils.rightPad("enable", maxLength) + ": " + staticResourceConfig.isEnable());
        props.addAll(handlers.stream()
                .map(handler -> StringUtils.rightPad(handler.getHostedPath(), maxLength) + ": " + handler.getLocationAbsPath())
                .collect(Collectors.toList())
        );
        BannerUtils.printConfig(log, "StaticResource配置",
                props.toArray(new String[0])
        );
        return create(rootPath, staticResourceConfig);
    }

    private static List<StaticResourceHandler> createHandler(String rootPath, List<StaticResourceConfig.ResourceMapping> mappings) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为 null");
        Assert.notNull(mappings, "参数 mappings 不能为 null");
        List<StaticResourceHandler> handlers = new ArrayList<>(mappings.size());
        for (StaticResourceConfig.ResourceMapping mapping : mappings) {
            handlers.add(new StaticResourceHandler(rootPath, mapping));
        }
        return handlers;
    }

    @Getter
    private final boolean enable;
    @Getter
    private final List<StaticResourceHandler> staticResourceHandlers;

    public StaticResourceFilter(boolean enable, List<StaticResourceHandler> staticResourceHandlers) {
        this.enable = enable;
        this.staticResourceHandlers = staticResourceHandlers;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        // 是否启用
        if (!enable) {
            ctx.next();
            return;
        }
        // 处理请求
        Resource resource = null;
        StaticResourceHandler useHandler = null;
        for (StaticResourceHandler handler : staticResourceHandlers) {
            resource = handler.getResource(ctx.req);
            if (resource != null) {
                useHandler = handler;
                break;
            }
        }
        // 未找到静态资源
        if (resource == null) {
            ctx.next();
            return;
        }
        // transformer(EncodedResourceResolver.EncodedResource | GzipResourceResolver.GzippedResource)
        // Header phase 判断资源是否发生变化
        if (useHandler.isUseLastModified() && new CheckResourceModified(ctx.req, ctx.res).checkNotModified(resource.lastModified())) {
            log.trace("Resource not modified");
            return;
        }
        // Apply cache settings, if any

        // Check the media type for the resource

        // Content phase

        ctx.next();
    }
}
