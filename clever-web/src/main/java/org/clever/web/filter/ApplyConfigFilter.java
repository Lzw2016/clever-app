package org.clever.web.filter;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.ResourcePathUtils;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.WebConfig;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * 应用web配置过滤器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 17:42 <br/>
 */
public class ApplyConfigFilter implements FilterRegistrar.FilterFuc {
    public static ApplyConfigFilter create(String rootPath, WebConfig.HttpConfig http) {
        return new ApplyConfigFilter(rootPath, http);
    }

    public static final String MULTIPART_CONFIG_ATTRIBUTE = "org.eclipse.jetty.multipartConfig";
    private final String rootPath;
    private final WebConfig.HttpConfig http;

    public ApplyConfigFilter(String rootPath, WebConfig.HttpConfig http) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(http, "参数 http 不能为null");
        this.rootPath = rootPath;
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
        // 文件上传设置
        String contentType = ctx.req.getContentType();
        WebConfig.HttpConfig.Multipart multipart = Optional.of(http.getMultipart()).orElse(new WebConfig.HttpConfig.Multipart());
        if (multipart.isEnabled() && StringUtils.startsWith(contentType, "multipart/")) {
            preUploadFunction(ctx.req, multipart);
        }
        ctx.next();
    }

    private void preUploadFunction(HttpServletRequest request, WebConfig.HttpConfig.Multipart multipart) {
        Object existingConfig = request.getAttribute(MULTIPART_CONFIG_ATTRIBUTE);
        if (existingConfig == null) {
            request.setAttribute(
                    MULTIPART_CONFIG_ATTRIBUTE,
                    new MultipartConfigElement(
                            ResourcePathUtils.getAbsolutePath(rootPath, multipart.getLocation()),
                            multipart.getMaxFileSize().toBytes(),
                            multipart.getMaxRequestSize().toBytes(),
                            (int) multipart.getFileSizeThreshold().toBytes()
                    )
            );
        }
    }
}
