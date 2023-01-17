package org.clever.web.filter;

import org.apache.commons.lang3.StringUtils;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.WebConfig;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 17:42 <br/>
 */
public class DefSettingFilter implements FilterRegistrar.FilterFuc {
    public static DefSettingFilter create(WebConfig.HttpConfig http) {
        return new DefSettingFilter(http);
    }

    private final WebConfig.HttpConfig http;

    public DefSettingFilter(WebConfig.HttpConfig http) {
        Assert.notNull(http, "参数 http 不能为null");
        this.http = http;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
        // 统一 ContentType
        if (StringUtils.isNotBlank(http.getDefaultContentType())) {
            ctx.res.setContentType(http.getDefaultContentType());
        }
        // 统一 CharacterEncoding
        if (StringUtils.isNotBlank(http.getDefaultCharacterEncoding())) {
            ctx.res.setCharacterEncoding(http.getDefaultCharacterEncoding());
        }
        ctx.next();
    }
}
