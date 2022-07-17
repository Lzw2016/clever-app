package org.clever.web.config;

import io.javalin.core.util.Header;
import io.javalin.http.ContentType;
import io.javalin.http.staticfiles.Location;
import lombok.Data;
import org.clever.util.unit.DataSize;

import java.time.Duration;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/17 13:33 <br/>
 */
@Data
public class HTTP {
    /**
     * 为响应生成 etag，默认：false (ETag是HTTP协议提供的一种Web缓存验证机制，并且允许客户端进行缓存协商。这就使得缓存变得更加高效，而且节省带宽)
     */
    private boolean autogenerateEtags = false;
    /**
     * 如果路径映射到不同的 HTTP 方法，则返回 405 而不是 404，默认：true
     */
    private boolean prefer405over404 = true;
    /**
     * 将所有 http 请求重定向到 https，默认：false
     */
    private boolean enforceSsl = false;
    /**
     * 默认内容类型，默认："application/json"
     */
    private String defaultContentType = ContentType.JSON;
    /**
     * 最大请求大小，默认：1MB (要么增加这个，要么使用 InputStream 来处理大的请求)
     */
    private DataSize maxRequestSize = DataSize.ofMegabytes(1);
    /**
     * 异步请求超时时间，默认：0s (0表示不超时)
     */
    private Duration asyncRequestTimeout = Duration.ofSeconds(0);
    /**
     * 花哨的 404 处理程序，在 "/path" 上返回 404 的指定文件(常用于单页面应用)
     */
    private List<SinglePageRoot> singlePageRoot = new ArrayList<>();
    /**
     * 静态资源映射
     */
    private List<StaticFile> staticFile = new ArrayList<>();
    /**
     * 通过 webjars 添加静态文件，默认：false
     */
    private boolean enableWebjars = false;
    /**
     * 为所有源启用 CORS，默认：false
     */
    private boolean enableCorsForAllOrigins = false;
    /**
     * 为指定源启用 CORS，默认：[]
     */
    private Set<String> enableCorsForOrigin = new HashSet<>();

    @Data
    public static class SinglePageRoot {
        /**
         * 服务端路径
         */
        private String hostedPath;
        /**
         * 文件路径
         */
        private String filePath;
        /**
         * 文件位置，默认：“CLASSPATH”
         */
        private Location location = Location.CLASSPATH;
    }

    @Data
    public static class StaticFile {
        /**
         * 服务端路径
         */
        private String hostedPath;
        /**
         * 文件路径
         */
        private String directory;
        /**
         * 文件位置，默认：“CLASSPATH”
         */
        private Location location = Location.CLASSPATH;
        /**
         * 预压缩，默认：false
         */
        private boolean preCompress = false;
        /**
         * 自定义响应头，默认：["Cache-Control": "max-age=0"]
         */
        private Map<String, String> headers = new HashMap<String, String>(1) {{
            put(Header.CACHE_CONTROL, "max-age=0");
        }};
    }
}
