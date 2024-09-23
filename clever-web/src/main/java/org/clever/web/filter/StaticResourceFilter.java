package org.clever.web.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.AppContextHolder;
import org.clever.core.Assert;
import org.clever.core.BannerUtils;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.StaticResourceConfig;
import org.clever.web.exception.GenericHttpException;
import org.clever.web.support.resource.StaticResourceHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 静态资源的访问Filter，参考Spring {@code ResourceHttpRequestHandler}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/22 22:00 <br/>
 */
@Getter
@Slf4j
public class StaticResourceFilter implements FilterRegistrar.FilterFuc {
    public static StaticResourceFilter create(String rootPath, StaticResourceConfig staticResourceConfig) {
        Assert.notNull(staticResourceConfig, "参数 staticResourceConfig 不能为 null");
        return new StaticResourceFilter(staticResourceConfig.isEnable(), createHandler(rootPath, staticResourceConfig.getMappings()));
    }

    public static StaticResourceFilter create(String rootPath, Environment environment) {
        StaticResourceConfig staticResourceConfig = Binder.get(environment).bind(StaticResourceConfig.PREFIX, StaticResourceConfig.class).orElseGet(StaticResourceConfig::new);
        AppContextHolder.registerBean("staticResourceConfig", staticResourceConfig, true);
        List<StaticResourceHandler> handlers = createHandler(rootPath, staticResourceConfig.getMappings());
        int maxLength = handlers.stream().map(StaticResourceHandler::getHostedPath).max(Comparator.comparingInt(String::length)).orElse("").length();
        List<String> logs = new ArrayList<>();
        logs.add(org.apache.commons.lang3.StringUtils.rightPad("enable", maxLength) + ": " + staticResourceConfig.isEnable());
        logs.addAll(handlers.stream().map(handler -> org.apache.commons.lang3.StringUtils.rightPad(handler.getHostedPath(), maxLength) + ": " + handler.getLocationAbsPath()).toList());
        if (staticResourceConfig.isEnable()) {
            BannerUtils.printConfig(log, "StaticResource配置", logs.toArray(new String[0]));
        }
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

    public static final Set<String> SUPPORTED_METHODS = new HashSet<>() {{
        add("GET");
        add("HEAD");
    }};

    private final boolean enable;
    private final List<StaticResourceHandler> staticResourceHandlers;

    public StaticResourceFilter(boolean enable, List<StaticResourceHandler> staticResourceHandlers) {
        this.enable = enable;
        this.staticResourceHandlers = staticResourceHandlers;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {
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
        // Supported methods
        checkRequest(ctx.req);
        // Header phase 判断资源是否发生变化
        if (useHandler.checkNotModified(ctx.req, ctx.res, resource.lastModified())) {
            log.trace("Resource not modified");
            return;
        }
        // Apply cache settings, if any
        useHandler.applyCacheControl(ctx.res);
        // Check the media type for the resource
        useHandler.setMediaType(ctx.req, ctx.res, resource);
        // Content phase
        if (ctx.req.getHeader(HttpHeaders.RANGE) == null) {
            write(ctx.res, resource);
        } else {
            try {
                List<HttpRange> httpRanges = createHttpHeaders(ctx.req).getRange();
                ctx.res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                List<ResourceRegion> resourceRegions = HttpRange.toResourceRegions(httpRanges, resource);
                if (resourceRegions.size() == 1) {
                    writeResourceRegion(ctx.res, resourceRegions.get(0));
                } else {
                    writeResourceRegions(ctx.res, resourceRegions);
                }
            } catch (IllegalArgumentException ex) {
                ctx.res.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
                ctx.res.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            }
        }
    }

    private void checkRequest(HttpServletRequest request) throws ServletException {
        // Check whether we should support the request method.
        String method = request.getMethod();
        if (!SUPPORTED_METHODS.contains(method.toUpperCase())) {
            throw new GenericHttpException(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed, Allowed Methods: " + org.apache.commons.lang3.StringUtils.join(SUPPORTED_METHODS, ", ")
            );
        }
    }

    private void write(HttpServletResponse response, Resource resource) throws IOException {
        try {
            try (InputStream in = resource.getInputStream()) {
                StreamUtils.copy(in, response.getOutputStream());
                response.getOutputStream().flush();
            } catch (NullPointerException ex) {
                // ignore, see SPR-13620
            }
        } catch (FileNotFoundException ex) {
            // ignore, see SPR-12999
        }
    }

    private void writeResourceRegion(HttpServletResponse response, ResourceRegion region) throws IOException {
        Assert.notNull(region, "ResourceRegion must not be null");
        long start = region.getPosition();
        long end = start + region.getCount() - 1;
        long resourceLength = region.getResource().contentLength();
        end = Math.min(end, resourceLength - 1);
        long rangeLength = end - start + 1;
        response.addHeader("Content-Range", "bytes " + start + '-' + end + '/' + resourceLength);
        response.setContentLength((int) rangeLength);
        try (InputStream in = region.getResource().getInputStream()) {
            StreamUtils.copyRange(in, response.getOutputStream(), start, end);
        }
        response.getOutputStream().flush();
    }

    private void writeResourceRegions(HttpServletResponse response, Collection<ResourceRegion> resourceRegions) throws IOException {
        Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
        MediaType contentType = tryParseMediaType(response.getContentType());
        String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
        response.setHeader(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
        OutputStream out = response.getOutputStream();
        Resource resource = null;
        InputStream in = null;
        long inputStreamPosition = 0;
        try {
            for (ResourceRegion region : resourceRegions) {
                long start = region.getPosition() - inputStreamPosition;
                if (start < 0 || resource != region.getResource()) {
                    if (in != null) {
                        in.close();
                    }
                    resource = region.getResource();
                    in = resource.getInputStream();
                    inputStreamPosition = 0;
                    start = region.getPosition();
                }
                long end = start + region.getCount() - 1;
                // Writing MIME header.
                println(out);
                print(out, "--" + boundaryString);
                println(out);
                if (contentType != null) {
                    print(out, "Content-Type: " + contentType);
                    println(out);
                }
                long resourceLength = region.getResource().contentLength();
                end = Math.min(end, resourceLength - inputStreamPosition - 1);
                print(out, "Content-Range: bytes " + region.getPosition() + '-' + (region.getPosition() + region.getCount() - 1) + '/' + resourceLength);
                println(out);
                println(out);
                // Printing content
                StreamUtils.copyRange(in, out, start, end);
                inputStreamPosition += (end + 1);
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
        println(out);
        print(out, "--" + boundaryString + "--");
        out.flush();
    }

    private static void println(OutputStream os) throws IOException {
        os.write('\r');
        os.write('\n');
    }

    private static void print(OutputStream os, String buf) throws IOException {
        os.write(buf.getBytes(StandardCharsets.US_ASCII));
    }

    private static HttpHeaders createHttpHeaders(HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (Enumeration<?> names = request.getHeaderNames(); names.hasMoreElements(); ) {
            String headerName = (String) names.nextElement();
            for (Enumeration<?> headerValues = request.getHeaders(headerName); headerValues.hasMoreElements(); ) {
                String headerValue = (String) headerValues.nextElement();
                httpHeaders.add(headerName, headerValue);
            }
        }
        // HttpServletRequest exposes some headers as properties: we should include those if not already present
        try {
            MediaType contentType = httpHeaders.getContentType();
            if (contentType == null) {
                String requestContentType = request.getContentType();
                if (StringUtils.hasLength(requestContentType)) {
                    contentType = MediaType.parseMediaType(requestContentType);
                    httpHeaders.setContentType(contentType);
                }
            }
            if (contentType != null && contentType.getCharset() == null) {
                String requestEncoding = request.getCharacterEncoding();
                if (StringUtils.hasLength(requestEncoding)) {
                    Charset charSet = Charset.forName(requestEncoding);
                    Map<String, String> params = new LinkedCaseInsensitiveMap<>();
                    params.putAll(contentType.getParameters());
                    params.put("charset", charSet.toString());
                    MediaType mediaType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
                    httpHeaders.setContentType(mediaType);
                }
            }
        } catch (InvalidMediaTypeException ex) {
            // Ignore: simply not exposing an invalid content type in HttpHeaders...
        }
        if (httpHeaders.getContentLength() < 0) {
            int requestContentLength = request.getContentLength();
            if (requestContentLength != -1) {
                httpHeaders.setContentLength(requestContentLength);
            }
        }
        return httpHeaders;
    }

    /**
     * 尝试将给定的字符串解析为单个 {@code MediaType}，解析失败返回 null
     */
    private static MediaType tryParseMediaType(String mediaType) {
        if (org.apache.commons.lang3.StringUtils.isBlank(mediaType)) {
            return null;
        }
        try {
            return MediaType.parseMediaType(mediaType);
        } catch (Exception e) {
            return null;
        }
    }
}
