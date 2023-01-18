package org.clever.web.config;

import lombok.Data;
import org.clever.web.http.HttpMethod;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * mvc 配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/06 15:14 <br/>
 */
@Data
public class MvcConfig {
    public static final String PREFIX = WebConfig.PREFIX + ".mvc";

    /**
     * 是否启用mvc功能
     */
    private boolean enable = true;
    /**
     * mvc接口前缀
     */
    private String path = "/";
    /**
     * mvc支持的Http Method
     */
    private Set<HttpMethod> httpMethod = new HashSet<HttpMethod>() {{
        add(HttpMethod.POST);
        add(HttpMethod.GET);
        add(HttpMethod.PUT);
        add(HttpMethod.PATCH);
        add(HttpMethod.DELETE);
        add(HttpMethod.HEAD);
        add(HttpMethod.TRACE);
        add(HttpMethod.OPTIONS);
    }};
    /**
     * mvc映射的 Method 的 package 默认前缀
     */
    private String packagePrefix = "";
    /**
     * 允许mvc调用的package前缀
     */
    private Set<String> allowPackages = new HashSet<>();
    /**
     * 热重载配置
     */
    private MvcConfig.HotReload hotReload = new MvcConfig.HotReload();

    @Data
    public static class HotReload {
        /**
         * 是否启用热重载模式
         */
        private boolean enable = false;
        /**
         * 文件检查时间间隔(默认1秒)
         */
        private Duration interval = Duration.ofSeconds(1);
        /**
         * 不使用热重载的package前缀
         */
        private Set<String> excludePackages = new HashSet<>();
        /**
         * 热重载class位置
         * <pre>
         * 1.classpath路径
         *   classpath:com/mycompany/**&#47;*.xml
         *   classpath*:com/mycompany/**&#47;*.xml
         * 2.本机绝对路径
         *   file:/home/www/public/
         *   file:D:/resources/static/
         * 3.本机相对路径
         *   ../public/static
         *   ./public
         *   ../../dist
         * </pre>
         */
        private List<String> locations = Arrays.asList(
                "./build/classes/java/main",
                "./build/classes/kotlin/main",
                "./build/classes/groovy/main",
                "./out/production/classes"
        );
    }
}
