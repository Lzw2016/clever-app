package org.clever.web.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.CorsConfig;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 21:58 <br/>
 */
@Slf4j
public class CorsFilter implements FilterRegistrar.FilterFuc {
    public static CorsFilter create(CorsConfig corsConfig) {
        Assert.notNull(corsConfig, "参数 corsConfig 不能为 null");
        return new CorsFilter(corsConfig);
    }

    public static CorsFilter create(Environment environment) {
        CorsConfig corsConfig = Binder.get(environment).bind(CorsConfig.PREFIX, CorsConfig.class).orElseGet(CorsConfig::new);
        BannerUtils.printConfig(log, "cors浏览器跨域配置",
                new String[]{
                        "cors",
                        "  enable               : " + corsConfig.isEnable(),
                        "  pathPattern          : " + StringUtils.join(corsConfig.getPathPattern(), " | "),
                        "  allowedOrigins       : " + StringUtils.join(corsConfig.getAllowedOrigins(), " | "),
                        "  allowedOriginPatterns: " + StringUtils.join(corsConfig.getAllowedOriginPatterns(), " | "),
                        "  allowedMethods       : " + StringUtils.join(corsConfig.getAllowedMethods(), " | "),
                        "  allowedHeaders       : " + StringUtils.join(corsConfig.getAllowedHeaders(), " | "),
                        "  exposedHeaders       : " + StringUtils.join(corsConfig.getExposedHeaders(), " | "),
                        "  allowCredentials     : " + corsConfig.getAllowCredentials(),
                        "  maxAge               : " + corsConfig.getMaxAge(),
                }
        );
        return create(corsConfig);
    }

    @Getter
    private final CorsConfig corsConfig;

    public CorsFilter(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws Exception {
        // 是否启用
        if (!corsConfig.isEnable()) {
            ctx.next();
            return;
        }
        ctx.next();
    }
}