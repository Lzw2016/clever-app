package org.clever.web.filter;

import jakarta.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.HttpConfig;
import org.clever.web.config.WebConfig;

import java.io.IOException;

/**
 * 应用web配置过滤器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 17:42 <br/>
 */
public class ApplyConfigFilter implements FilterRegistrar.FilterFuc {
    public static ApplyConfigFilter create(WebConfig webConfig) {
        return new ApplyConfigFilter(webConfig);
    }

    private final WebConfig webConfig;

    public ApplyConfigFilter(WebConfig webConfig) {
        Assert.notNull(webConfig, "参数 webConfig 不能为null");
        this.webConfig = webConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        final HttpConfig http = webConfig.getHttp();
        if (http != null) {
            // 统一 ContentType
            if (StringUtils.isNotBlank(http.getDefaultContentType())) {
                ctx.res.setContentType(http.getDefaultContentType());
            }
            // 统一 CharacterEncoding
            if (StringUtils.isNotBlank(http.getDefaultCharacterEncoding())) {
                ctx.res.setCharacterEncoding(http.getDefaultCharacterEncoding());
            }
        }
        ctx.next();
    }
}
