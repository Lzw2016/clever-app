package org.clever.web.filter;

import jakarta.servlet.ServletException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppContextHolder;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;
import org.clever.core.SystemClock;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.EchoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.util.List;

/**
 * 简单输出请求访问日志(仅记录请求路径和耗时)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 21:56 <br/>
 */
@Getter
@Slf4j
public class EchoFilter implements FilterRegistrar.FilterFuc {
    public static EchoFilter create(EchoConfig echoConfig) {
        Assert.notNull(echoConfig, "参数 echoConfig 不能为 null");
        return new EchoFilter(echoConfig);
    }

    public static EchoFilter create(Environment environment) {
        EchoConfig echoConfig = Binder.get(environment).bind(EchoConfig.PREFIX, EchoConfig.class).orElseGet(EchoConfig::new);
        AppContextHolder.registerBean("echoConfig", echoConfig, true);
        if (echoConfig.isEnable()) {
            // noinspection ConstantValue
            BannerUtils.printConfig(log, "Echo配置",
                new String[]{
                    "enable     : " + echoConfig.isEnable(),
                    "ignorePaths: " + StringUtils.join(echoConfig.getIgnorePaths(), " | ")
                }
            );
        }
        return create(echoConfig);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("ECHO");
    private static final PathMatcher MATCHER = new AntPathMatcher();

    private final EchoConfig echoConfig;

    public EchoFilter(EchoConfig echoConfig) {
        this.echoConfig = echoConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws ServletException, IOException {
        // 是否启用
        if (!echoConfig.isEnable()) {
            ctx.next();
            return;
        }
        final String reqPath = ctx.req.getPathInfo();
        // 在 ignore 出现的路径，忽略掉
        List<String> ignore = echoConfig.getIgnorePaths();
        if (ignore != null && !ignore.isEmpty()) {
            for (String path : ignore) {
                if (MATCHER.match(path, reqPath)) {
                    ctx.next();
                    return;
                }
            }
        }
        // ECHO 逻辑
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
            LOGGER.info(sb.append(" | [").append(cost).append("ms").append("] | [").append(ctx.res.getStatus()).append("]").toString());
        }
    }
}
