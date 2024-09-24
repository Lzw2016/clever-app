package org.clever.web.filter;

import io.javalin.http.util.MultipartUtil;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.core.ResourcePathUtils;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.HttpConfig;
import org.clever.web.config.WebConfig;

import java.io.IOException;
import java.util.Optional;

/**
 * 应用web配置过滤器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 17:42 <br/>
 */
public class ApplyConfigFilter implements FilterRegistrar.FilterFuc {
    public static ApplyConfigFilter create(String rootPath, WebConfig webConfig) {
        return new ApplyConfigFilter(rootPath, webConfig);
    }

    private final String rootPath;
    private final WebConfig webConfig;

    public ApplyConfigFilter(String rootPath, WebConfig webConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(webConfig, "参数 webConfig 不能为null");
        this.rootPath = rootPath;
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
            // 文件上传设置
            String contentType = ctx.req.getContentType();
            HttpConfig.Multipart multipart = Optional.of(http.getMultipart()).orElse(new HttpConfig.Multipart());
            if (StringUtils.startsWith(contentType, "multipart/")) {
                preUploadFunction(ctx.req, multipart);
            }
        }
        ctx.next();
    }

    /**
     * 参考 {@link org.eclipse.jetty.server.Request#getParts()}
     */
    private void preUploadFunction(HttpServletRequest request, HttpConfig.Multipart multipart) {
        Object existingConfig = request.getAttribute(MultipartUtil.MULTIPART_CONFIG_ATTRIBUTE);
        if (existingConfig == null) {
            request.setAttribute(
                MultipartUtil.MULTIPART_CONFIG_ATTRIBUTE,
                new MultipartConfigElement(
                    ResourcePathUtils.getAbsolutePath(rootPath, multipart.getLocation()),
                    multipart.getMaxFileSize().toBytes(),
                    multipart.getMaxTotalRequestSize().toBytes(),
                    (int) multipart.getMaxInMemoryFileSize().toBytes()
                )
            );
        }
    }
}
