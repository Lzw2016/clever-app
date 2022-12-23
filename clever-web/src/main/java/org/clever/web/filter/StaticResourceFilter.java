package org.clever.web.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.core.tuples.TupleTwo;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.StaticResourceConfig;
import org.clever.web.utils.PathUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 22:00 <br/>
 */
@Slf4j
public class StaticResourceFilter implements FilterRegistrar.FilterFuc {
    public static StaticResourceFilter create(String rootPath, StaticResourceConfig staticResourceConfig) {
        Assert.notNull(staticResourceConfig, "参数 staticResourceConfig 不能为 null");
        return new StaticResourceFilter(rootPath, staticResourceConfig);
    }

    public static StaticResourceFilter create(String rootPath, Environment environment) {
        StaticResourceConfig staticResourceConfig = Binder.get(environment)
                .bind(StaticResourceConfig.PREFIX, StaticResourceConfig.class)
                .orElseGet(StaticResourceConfig::new);
        List<TupleTwo<String, String>> mappings = new ArrayList<>(staticResourceConfig.getMappings().size());
        for (StaticResourceConfig.ResourceMapping mapping : staticResourceConfig.getMappings()) {
            mappings.add(TupleTwo.creat(mapping.getHostedPath(), PathUtils.getAbsolutePath(rootPath, mapping.getLocation())));
        }
        int maxLength = mappings.stream().map(TupleTwo::getValue1).max(Comparator.comparingInt(String::length)).orElse("").length();
        BannerUtils.printConfig(log, "StaticResource配置",
                mappings.stream()
                        .map(tuple -> StringUtils.rightPad(tuple.getValue1(), maxLength) + ": " + tuple.getValue2())
                        .toArray(String[]::new)
        );
        return create(rootPath, staticResourceConfig);
    }

    /**
     * 全局的资源根路径
     */
    @Getter
    private final String rootPath;
    @Getter
    private final StaticResourceConfig staticResourceConfig;

    public StaticResourceFilter(String rootPath, StaticResourceConfig staticResourceConfig) {
        this.rootPath = rootPath;
        this.staticResourceConfig = staticResourceConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        // 是否启用
        if (!staticResourceConfig.isEnable()) {
            ctx.next();
            return;
        }
        ctx.next();
    }
}
