package org.clever.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.BannerUtils;
import org.clever.core.SystemClock;
import org.clever.core.env.Environment;
import org.clever.util.AntPathMatcher;
import org.clever.util.Assert;
import org.clever.util.PathMatcher;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.EchoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 21:56 <br/>
 */
@Slf4j
public class EchoFilter implements FilterRegistrar.FilterFuc {
    public static EchoFilter create(EchoConfig echoConfig) {
        Assert.notNull(echoConfig, "参数 echoConfig 不能为 null");
        return new EchoFilter(echoConfig);
    }

    public static EchoFilter create(Environment environment) {
        EchoConfig echoConfig = Binder.get(environment).bind(EchoConfig.PREFIX, EchoConfig.class).orElseGet(EchoConfig::new);
        BannerUtils.printConfig(log, "Echo配置",
                new String[]{
                        "ignorePaths: " + StringUtils.join(echoConfig.getIgnorePaths(), " | ")
                }
        );
        return create(echoConfig);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("ECHO");
    private static final PathMatcher MATCHER = new AntPathMatcher();

    private final EchoConfig echoConfig;

    public EchoFilter(EchoConfig echoConfig) {
        this.echoConfig = echoConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) {
        List<String> ignore = echoConfig.getIgnorePaths();
        // 在 ignore 出现的路径，忽略掉
        if (ignore != null && !ignore.isEmpty()) {
            for (String path : ignore) {
                if (MATCHER.match(path, ctx.req.getRequestURI())) {
                    ctx.next();
                    return;
                }
            }
        }
        // ECHO
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.rightPad(ctx.req.getMethod(), 8)).append(ctx.req.getRequestURI());
        if (ctx.req.getQueryString() != null) {
            sb.append("?").append(ctx.req.getQueryString());
        }
        final long startTime = SystemClock.now();
        LOGGER.info(sb.toString());
        try {
            ctx.next();
        } finally {
            long cost = SystemClock.now() - startTime;
            LOGGER.info(sb.append(" | ").append(cost).append("ms").toString());
        }
    }
}
