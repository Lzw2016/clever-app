package org.clever.web.support.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Assert;
import org.clever.core.ResourcePathUtils;
import org.clever.web.config.StaticResourceConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.HttpResource;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务端静态资源处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 13:53 <br/>
 */
@Slf4j
public class StaticResourceHandler {
    public static final String HEADER_PRAGMA = "Pragma";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /**
     * 服务端路径前缀
     */
    @Getter
    private final String hostedPath;
    /**
     * 静态资源位置
     */
    @Getter
    private final Resource location;
    /**
     * 是否需要检查资源被修改(checkNotModified)
     */
    @Setter
    @Getter
    private boolean useLastModified = true;
    /**
     * 服务端资源缓存控制
     */
    @Getter
    private final ResourceCacheControl resourceCacheControl;
    /**
     * 用于自定义“Content-Type” {@code Map<资源后缀名(全小写), MediaType>}
     */
    private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

    public StaticResourceHandler(String rootPath, StaticResourceConfig.ResourceMapping resourceMapping) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(resourceMapping, "参数 resourceMapping 不能为 null");
        this.hostedPath = resourceMapping.getHostedPath();
        this.location = ResourcePathUtils.getResource(rootPath, resourceMapping.getLocation());
        Duration cachePeriod = Optional.of(resourceMapping.getCachePeriod()).orElse(Duration.ofSeconds(0));
        CacheControl cacheControl = cachePeriod.isZero() ? CacheControl.noStore() : CacheControl.maxAge(cachePeriod);
        this.resourceCacheControl = new ResourceCacheControl(cacheControl);
    }

    /**
     * 静态资源绝对路径
     */
    public String getLocationAbsPath() {
        return ResourcePathUtils.getAbsolutePath(location);
    }

    /**
     * 自定义MediaType: {@code Map<资源后缀名(全小写), MediaType>}
     */
    public void setMediaTypes(Map<String, MediaType> mediaTypes) {
        mediaTypes.forEach((ext, mediaType) -> this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), mediaType));
    }

    /**
     * 获取静态资源
     *
     * @param request http请求
     * @return 不匹配或者不存在则返回 null
     */
    public Resource getResource(HttpServletRequest request) throws IOException {
        String path = request.getPathInfo();
        // 判断请求前缀是否满足配置
        if (org.apache.commons.lang3.StringUtils.isBlank(path) || !path.startsWith(hostedPath)) {
            return null;
        }
        // 处理请求path获取resourcePath
        path = path.substring(hostedPath.length());
        path = processPath(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // 处理path的编码问题
        if (isInvalidPath(path) || isInvalidEncodedPath(path)) {
            return null;
        }
        String resourcePath = encodeOrDecodeIfNecessary(path);
        // 如果path完全匹配且location就是一个存在的文件就直接返回location
        if (org.apache.commons.lang3.StringUtils.isBlank(path) && ResourcePathUtils.isExistsFile(location)) {
            return location;
        }
        Resource resource = location.createRelative(resourcePath);
        if (!resource.isReadable() || !isResourceUnderLocation(location, resource)) {
            return null;
        }
        return resource;
    }

    /**
     * 判断资源是否发生变化，如果未发送变化直接响应客户端
     */
    public boolean checkNotModified(HttpServletRequest req, HttpServletResponse res, long lastModifiedTimestamp) {
        return useLastModified && new CheckResourceModified(req, res).checkNotModified(lastModifiedTimestamp);
    }

    /**
     * 根据此生成器的设置准备给定的响应。应用为此生成器指定的缓存秒数。
     */
    public void applyCacheControl(HttpServletResponse res) {
        resourceCacheControl.prepareResponse(res);
    }

    /**
     * 为当前请求设置 MediaType
     *
     * @param request  http请求
     * @param response http响应
     * @param resource 已识别的资源
     */
    public void setMediaType(HttpServletRequest request, HttpServletResponse response, Resource resource) {
        MediaType mediaType = getMediaType(request, resource);
        if (mediaType != null) {
            setHeaders(response, resource, mediaType);
        }
    }

    /**
     * 获取当前Resource对应的MediaType
     *
     * @param request  http请求
     * @param resource 已识别的资源
     */
    private MediaType getMediaType(HttpServletRequest request, Resource resource) {
        MediaType result = null;
        String mimeType = request.getServletContext().getMimeType(resource.getFilename());
        if (StringUtils.hasText(mimeType)) {
            result = MediaType.parseMediaType(mimeType);
        }
        if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
            MediaType mediaType = null;
            String filename = resource.getFilename();
            String ext = StringUtils.getFilenameExtension(filename);
            if (ext != null) {
                mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
            }
            if (mediaType == null) {
                List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
                if (!CollectionUtils.isEmpty(mediaTypes)) {
                    mediaType = mediaTypes.get(0);
                }
            }
            if (mediaType != null) {
                result = mediaType;
            }
        }
        return result;
    }

    /**
     * 在给定的 servlet 响应上设置标头。为 GET 请求和 HEAD 请求调用
     *
     * @param response  当前 http 响应
     * @param resource  已识别的资源
     * @param mediaType 资源的媒体类型
     */
    private void setHeaders(HttpServletResponse response, Resource resource, MediaType mediaType) {
        if (mediaType != null) {
            response.setContentType(mediaType.toString());
        }
        if (resource instanceof HttpResource httpResource) {
            HttpHeaders resourceHeaders = httpResource.getResponseHeaders();
            resourceHeaders.forEach((headerName, headerValues) -> {
                boolean first = true;
                for (String headerValue : headerValues) {
                    if (first) {
                        response.setHeader(headerName, headerValue);
                    } else {
                        response.addHeader(headerName, headerValue);
                    }
                    first = false;
                }
            });
        }
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
    }

    private boolean isResourceUnderLocation(Resource location, Resource resource) throws IOException {
        if (resource.getClass() != location.getClass()) {
            return false;
        }
        String resourcePath;
        String locationPath;
        if (resource instanceof UrlResource) {
            resourcePath = resource.getURL().toExternalForm();
            locationPath = StringUtils.cleanPath(location.getURL().toString());
        } else if (resource instanceof ClassPathResource) {
            resourcePath = ((ClassPathResource) resource).getPath();
            locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
        } else {
            resourcePath = resource.getURL().getPath();
            locationPath = StringUtils.cleanPath(location.getURL().getPath());
        }
        if (locationPath.equals(resourcePath)) {
            return true;
        }
        locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
        return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
    }

    private String encodeOrDecodeIfNecessary(String path) {
        if (org.apache.commons.lang3.StringUtils.isBlank(path)) {
            return "";
        }
        Charset charset = StandardCharsets.UTF_8;
        StringBuilder sb = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            String value = UriUtils.encodePath(tokenizer.nextToken(), charset);
            sb.append(value);
            sb.append('/');
        }
        if (!path.endsWith("/")) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private boolean isInvalidEncodedPath(String path) {
        if (path.contains("%")) {
            try {
                // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
                String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
                decodedPath = processPath(decodedPath);
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                // May not be possible to decode...
            }
        }
        return false;
    }

    private boolean isInvalidPath(String path) {
        if (path.contains("WEB-INF") || path.contains("META-INF")) {
            if (log.isWarnEnabled()) {
                log.warn("Path with 'WEB-INF' or 'META-INF': [{}]", path);
            }
            return true;
        }
        if (path.contains(":/")) {
            String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
            if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                if (log.isWarnEnabled()) {
                    log.warn("Path represents URL or has 'url:' prefix: [{}]", path);
                }
                return true;
            }
        }
        if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
            if (log.isWarnEnabled()) {
                log.warn("Path contains '../' after call to StringUtils#cleanPath: [{}]", path);
            }
            return true;
        }
        return false;
    }

    /**
     * 处理给定的资源路径。默认实现替换：
     * <pre>
     * 1.带正斜杠的反斜杠
     * 2.使用单个斜线重复出现斜线
     * 3.前导斜杠和控制字符（00-1F 和 7F）与单个“/”或“”的任意组合。例如{@code "  / // foo/bar"}变成{@code "/foo/bar"}
     * </pre>
     */
    private String processPath(String path) {
        path = StringUtils.replace(path, "\\", "/");
        path = cleanDuplicateSlashes(path);
        return cleanLeadingSlash(path);
    }

    private String cleanDuplicateSlashes(String path) {
        StringBuilder sb = null;
        char prev = 0;
        for (int i = 0; i < path.length(); i++) {
            char curr = path.charAt(i);
            try {
                if ((curr == '/') && (prev == '/')) {
                    if (sb == null) {
                        sb = new StringBuilder(path.substring(0, i));
                    }
                    continue;
                }
                if (sb != null) {
                    sb.append(path.charAt(i));
                }
            } finally {
                prev = curr;
            }
        }
        return sb != null ? sb.toString() : path;
    }

    private String cleanLeadingSlash(String path) {
        boolean slash = false;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                slash = true;
            } else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
                if (i == 0 || (i == 1 && slash)) {
                    return path;
                }
                return (slash ? "/" + path.substring(i) : path.substring(i));
            }
        }
        return (slash ? "/" : "");
    }

    /**
     * 检查资源是否被修改过
     */
    public static class CheckResourceModified {
        private static final List<String> SAFE_METHODS = Arrays.asList("GET", "HEAD");
        /**
         * 模式匹配ETag标题中的多个字段值，如 "If-Match", "If-None-Match".
         *
         * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
         */
        private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W/)?(\"[^\"]*\"))\\s*,?");
        /**
         * HTTP RFC中指定的日期格式
         *
         * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
         */
        private static final String[] DATE_FORMATS = new String[]{
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM dd HH:mm:ss yyyy"
        };
        private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

        private boolean notModified = false;
        private final HttpServletRequest req;
        private final HttpServletResponse res;

        public CheckResourceModified(HttpServletRequest req, HttpServletResponse res) {
            Assert.notNull(req, "参数 req 不能为 null");
            Assert.notNull(res, "参数 res 不能为 null");
            this.req = req;
            this.res = res;
        }

        /**
         * 判断资源是否发生变化
         */
        public boolean checkNotModified(long lastModifiedTimestamp) {
            return checkNotModified(null, lastModifiedTimestamp);
        }

        /**
         * 判断资源是否发生变化
         */
        public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
            if (this.notModified || HttpStatus.OK.value() != res.getStatus()) {
                return this.notModified;
            }
            // Evaluate conditions in order of precedence. See https://tools.ietf.org/html/rfc7232#section-6
            if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
                if (this.notModified) {
                    res.setStatus(HttpStatus.PRECONDITION_FAILED.value());
                }
                return this.notModified;
            }
            boolean validated = validateIfNoneMatch(etag);
            if (!validated) {
                validateIfModifiedSince(lastModifiedTimestamp);
            }
            // Update response
            boolean isHttpGetOrHead = SAFE_METHODS.contains(req.getMethod());
            if (this.notModified) {
                res.setStatus(isHttpGetOrHead ? HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
            }
            if (isHttpGetOrHead) {
                if (lastModifiedTimestamp > 0 && parseDateValue(res.getHeader(HttpHeaders.LAST_MODIFIED)) == -1) {
                    res.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
                }
                if (StringUtils.hasLength(etag) && res.getHeader(HttpHeaders.ETAG) == null) {
                    res.setHeader(HttpHeaders.ETAG, padEtagIfNecessary(etag));
                }
            }
            return this.notModified;
        }

        private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
            if (lastModifiedTimestamp < 0) {
                return false;
            }
            long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
            if (ifUnmodifiedSince == -1) {
                return false;
            }
            // We will perform this validation...
            this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
            return true;
        }

        private boolean validateIfNoneMatch(String etag) {
            if (!StringUtils.hasLength(etag)) {
                return false;
            }
            Enumeration<String> ifNoneMatch;
            try {
                ifNoneMatch = req.getHeaders(HttpHeaders.IF_NONE_MATCH);
            } catch (IllegalArgumentException ex) {
                return false;
            }
            if (!ifNoneMatch.hasMoreElements()) {
                return false;
            }
            // We will perform this validation...
            etag = padEtagIfNecessary(etag);
            if (etag.startsWith("W/")) {
                etag = etag.substring(2);
            }
            while (ifNoneMatch.hasMoreElements()) {
                String clientETags = ifNoneMatch.nextElement();
                Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
                // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
                while (etagMatcher.find()) {
                    if (StringUtils.hasLength(etagMatcher.group()) && etag.equals(etagMatcher.group(3))) {
                        this.notModified = true;
                        break;
                    }
                }
            }
            return true;
        }

        private String padEtagIfNecessary(String etag) {
            if (!StringUtils.hasLength(etag)) {
                return etag;
            }
            if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
                return etag;
            }
            return "\"" + etag + "\"";
        }

        private boolean validateIfModifiedSince(long lastModifiedTimestamp) {
            if (lastModifiedTimestamp < 0) {
                return false;
            }
            long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
            if (ifModifiedSince == -1) {
                return false;
            }
            // We will perform this validation...
            this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
            return true;
        }

        private long parseDateHeader(String headerName) {
            long dateValue = -1;
            try {
                dateValue = req.getDateHeader(headerName);
            } catch (IllegalArgumentException ex) {
                String headerValue = req.getHeader(headerName);
                // Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
                if (headerValue != null) {
                    int separatorIndex = headerValue.indexOf(';');
                    if (separatorIndex != -1) {
                        String datePart = headerValue.substring(0, separatorIndex);
                        dateValue = parseDateValue(datePart);
                    }
                }
            }
            return dateValue;
        }

        private long parseDateValue(String headerValue) {
            if (headerValue == null) {
                // No header value sent at all
                return -1;
            }
            if (headerValue.length() >= 3) {
                // Short "0" or "-1" like values are never valid HTTP date headers...
                // Let's only bother with SimpleDateFormat parsing for long enough values.
                for (String dateFormat : DATE_FORMATS) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                    simpleDateFormat.setTimeZone(GMT);
                    try {
                        return simpleDateFormat.parse(headerValue).getTime();
                    } catch (ParseException ex) {
                        // ignore
                    }
                }
            }
            return -1;
        }
    }

    /**
     * 客户端资源缓存控制
     */
    public static class ResourceCacheControl {
        private final CacheControl cacheControl;
        private String[] varyByRequestHeaders;

        public ResourceCacheControl(CacheControl cacheControl) {
            Assert.notNull(cacheControl, "参数 cacheControl 不能为 null");
            this.cacheControl = cacheControl;
        }

        /**
         * 根据此生成器的设置准备给定的响应。应用为此生成器指定的缓存秒数。
         *
         * @param res 当前HTTP响应
         */
        public void prepareResponse(HttpServletResponse res) {
            if (log.isTraceEnabled()) {
                log.trace("Applying default {}", this.cacheControl);
            }
            applyCacheControl(res, this.cacheControl);
            if (this.varyByRequestHeaders != null) {
                for (String value : getVaryRequestHeadersToAdd(res, this.varyByRequestHeaders)) {
                    res.addHeader("Vary", value);
                }
            }
        }

        /**
         * 配置一个或多个请求标头名称（例如“Accept-Language”）以添加到“Vary”响应标头，
         * 以通知客户端响应受内容协商和基于给定请求标头值的差异的影响。
         * 只有在响应“Vary”标头中不存在时，才会添加已配置的请求标头名称。
         *
         * @param varyByRequestHeaders 一个或多个请求头名称
         */
        public void setVaryByRequestHeaders(String... varyByRequestHeaders) {
            this.varyByRequestHeaders = varyByRequestHeaders;
        }

        /**
         * 根据给定设置设置HTTP Cache-Control标头
         *
         * @param response     当前HTTP响应
         * @param cacheControl 预配置的缓存控制设置
         */
        private void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
            String ccValue = cacheControl.getHeaderValue();
            if (ccValue != null) {
                // Set computed HTTP 1.1 Cache-Control header
                response.setHeader(HEADER_CACHE_CONTROL, ccValue);
                if (response.containsHeader(HEADER_PRAGMA)) {
                    // Reset HTTP 1.0 Pragma header if present
                    response.setHeader(HEADER_PRAGMA, "");
                }
                if (response.containsHeader(HEADER_EXPIRES)) {
                    // Reset HTTP 1.0 Expires header if present
                    response.setHeader(HEADER_EXPIRES, "");
                }
            }
        }

        private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response, String[] varyByRequestHeaders) {
            if (!response.containsHeader(HttpHeaders.VARY)) {
                return Arrays.asList(varyByRequestHeaders);
            }
            Collection<String> result = new ArrayList<>(varyByRequestHeaders.length);
            Collections.addAll(result, varyByRequestHeaders);
            for (String header : response.getHeaders(HttpHeaders.VARY)) {
                for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
                    if ("*".equals(existing)) {
                        return Collections.emptyList();
                    }
                    for (String value : varyByRequestHeaders) {
                        if (value.equalsIgnoreCase(existing)) {
                            result.remove(value);
                        }
                    }
                }
            }
            return result;
        }
    }
}
