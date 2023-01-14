package org.clever.web.filter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.util.AntPathMatcher;
import org.clever.util.Assert;
import org.clever.util.PathMatcher;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.CorsConfig;
import org.clever.web.http.HttpStatus;
import org.clever.web.support.cors.CorsProcessor;
import org.clever.web.support.cors.DefaultCorsProcessor;
import org.clever.web.utils.CorsUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * 跨域处理 Filter
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 21:58 <br/>
 */
@Slf4j
public class CorsFilter implements FilterRegistrar.FilterFuc {
    public static CorsFilter create(CorsConfig corsConfig) {
        return new CorsFilter(corsConfig);
    }

    public static CorsFilter create(Environment environment) {
        CorsConfig corsConfig = Binder.get(environment).bind(CorsConfig.PREFIX, CorsConfig.class).orElseGet(CorsConfig::new);
        AppContextHolder.registerBean("corsConfig", corsConfig, true);
        BannerUtils.printConfig(log, "cors浏览器跨域配置",
                new String[]{
                        "cors:",
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

    private static final PathMatcher MATCHER = new AntPathMatcher();

    @Getter
    private final CorsConfig corsConfig;
    private final CorsProcessor corsProcessor = new DefaultCorsProcessor();

    public CorsFilter(CorsConfig corsConfig) {
        Assert.notNull(corsConfig, "参数 corsConfig 不能为 null");
        this.corsConfig = corsConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws ServletException, IOException {
        // 是否启用
        if (!corsConfig.isEnable()) {
            ctx.next();
            return;
        }
        final String reqPath = ctx.req.getPathInfo();
        // 当前请求路径是否支持跨域
        boolean match = false;
        List<String> pathPattern = corsConfig.getPathPattern();
        if (pathPattern != null && !pathPattern.isEmpty()) {
            for (String path : pathPattern) {
                if (MATCHER.match(path, reqPath)) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            ctx.next();
            return;
        }
        // 跨域处理
        boolean supportCors = corsProcessor.processRequest(corsConfig, ctx.req, ctx.res);
        if (supportCors) {
            boolean preFlightRequest = CorsUtils.isPreFlightRequest(ctx.req);
            if (preFlightRequest) {
                // 是预检请求(OPTIONS)
                ctx.res.setStatus(HttpStatus.OK.value());
                ctx.res.getWriter().flush();
                return;
            }
        } else {
            // 不支持跨域
            return;
        }
        ctx.next();
    }
}
