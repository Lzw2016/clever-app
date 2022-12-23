package org.clever.web.config;

import lombok.Data;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/23 17:26 <br/>
 */
@Data
public class StaticResourceConfig {
    public static final String PREFIX = WebConfig.PREFIX + ".resources";

    /**
     * 启用 StaticFileFilter
     */
    private boolean enable = false;
    /**
     * 静态资源映射
     */
    private List<ResourceMapping> mappings = Collections.emptyList();

    @Data
    public static class ResourceMapping {
        /**
         * 服务端路径前缀<br/>
         * <pre>
         * /static/
         * /dist/
         * /assets/
         * </pre>
         */
        private String hostedPath;
        /**
         * 静态资源本机路径<br/>
         * <pre>
         * 1.classpath路径
         *   classpath:/public/
         *   classpath:/META-INF/resources/
         * 2.本机绝对路径
         *   file:/home/www/public/
         *   file:D:/resources/static/
         * 3.本机相对路径
         *   ../public/static
         *   ./public
         *   ../../dist
         * </pre>
         */
        private String location;
        /**
         * 缓存时间(单位：秒，小于等于0表示不缓存)
         */
        private Duration cachePeriod = Duration.ofSeconds(0);
    }

//    private  String[] pathPatterns;
//    private  List<String> locationValues = new ArrayList<>();
//    private  List<Resource> locationsResources = new ArrayList<>();
//    private Integer cachePeriod;
//    private CacheControl cacheControl;
//    private ResourceChainRegistration resourceChainRegistration;
//    private boolean useLastModified = true;
//    private boolean optimizeLocations = false;
}
