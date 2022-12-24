package org.clever.web.http;

import io.javalin.http.ContentType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.io.ClassPathResource;
import org.clever.core.io.Resource;
import org.clever.core.io.UrlResource;
import org.clever.util.ResourceUtils;
import org.clever.util.StringUtils;
import org.clever.web.config.StaticResourceConfig;
import org.clever.web.utils.PathUtils;
import org.clever.web.utils.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 服务端静态资源处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 13:53 <br/>
 */
@Slf4j
public class StaticResourceHandler {
    /**
     * HTTP method "GET".
     */
    public static final String METHOD_GET = "GET";
    /**
     * HTTP method "HEAD".
     */
    public static final String METHOD_HEAD = "HEAD";
    /**
     * HTTP method "POST".
     */
    public static final String METHOD_POST = "POST";
    public static final String HEADER_PRAGMA = "Pragma";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    /**
     * {@code Map<资源后缀名, ContentType>}
     */
    public static final Map<String, ContentType> CONTENT_TYPE_MAP = new HashMap<>(4);

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
     *
     */
    @Getter
    private final CacheControl cacheControl;
    /**
     *
     */
    @Getter
    private boolean useLastModified = true;
    /**
     *
     */
    @Getter
    private boolean optimizeLocations = false;

    public StaticResourceHandler(String rootPath, StaticResourceConfig.ResourceMapping resourceMapping) {
        this.hostedPath = resourceMapping.getHostedPath();
        this.location = PathUtils.getResource(rootPath, resourceMapping.getLocation());
        Duration cachePeriod = resourceMapping.getCachePeriod();
        if (cachePeriod != null && !cachePeriod.isZero()) {
            cacheControl = CacheControl.maxAge(cachePeriod);
        } else {
            cacheControl = CacheControl.noStore();
        }
    }

    /**
     * 获取静态资源
     *
     * @param request http请求
     * @return 不匹配或者不存在则返回 null
     */
    public Resource getResource(HttpServletRequest request) throws Exception {
        String path = request.getPathInfo();
        // 判断请求前缀是否满足配置
        if (StringUtils.isBlank(path) || !path.startsWith(hostedPath)) {
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
        if (StringUtils.isBlank(path) && PathUtils.isExistsFile(location)) {
            return location;
        }
        Resource resource = location.createRelative(resourcePath);
        if (!resource.isReadable() || !isResourceUnderLocation(location, resource)) {
            return null;
        }
        return resource;
    }

    /**
     * 静态资源绝对路径
     */
    public String getLocationAbsPath() {
        return PathUtils.getAbsolutePath(location);
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
        if (StringUtils.isBlank(path)) {
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
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
                decodedPath = processPath(decodedPath);
                if (isInvalidPath(decodedPath)) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                // May not be possible to decode...
            } catch (UnsupportedEncodingException ex) {
                // Should never happen...
            }
        }
        return false;
    }

    private boolean isInvalidPath(String path) {
        if (path.contains("WEB-INF") || path.contains("META-INF")) {
            if (log.isWarnEnabled()) {
                log.warn("Path with \"WEB-INF\" or \"META-INF\": [" + path + "]");
            }
            return true;
        }
        if (path.contains(":/")) {
            String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
            if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                if (log.isWarnEnabled()) {
                    log.warn("Path represents URL or has \"url:\" prefix: [" + path + "]");
                }
                return true;
            }
        }
        if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
            if (log.isWarnEnabled()) {
                log.warn("Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]");
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
}
