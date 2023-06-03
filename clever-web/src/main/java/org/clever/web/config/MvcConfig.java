package org.clever.web.config;

import lombok.Data;
import org.clever.transaction.TransactionDefinition;
import org.clever.transaction.annotation.Isolation;
import org.clever.transaction.annotation.Propagation;
import org.clever.web.http.HttpMethod;

import java.time.Duration;
import java.util.*;

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
     * 解析 HandlerMethod 时 path 与 package 的映射关系
     */
    private List<PackageMapping> packageMapping = new ArrayList<>();
    /**
     * 允许mvc调用的package前缀
     */
    private Set<String> allowPackages = new HashSet<>();
    /**
     * 默认的事务配置
     */
    private TransactionalConfig defTransactional = new TransactionalConfig();
    /**
     * 热重载配置
     */
    private HotReload hotReload = new HotReload();

    @Data
    public static class PackageMapping {
        private String pathPrefix;
        private String packagePrefix;

        @Override
        public String toString() {
            return pathPrefix + '=' + packagePrefix;
        }
    }

    @Data
    public static class TransactionalConfig {
        /**
         * 使用 {@code Transactional} 时的默认 datasource 值
         */
        private List<String> defDatasource = new ArrayList<>();
        /**
         * 要启用事务的数据源
         */
        private List<String> datasource = new ArrayList<>();
        /**
         * 事务传播性
         */
        private Propagation propagation = Propagation.REQUIRED;
        /**
         * 事务隔离级别
         */
        private Isolation isolation = Isolation.DEFAULT;
        /**
         * 事务超时时间
         */
        private int timeout = TransactionDefinition.TIMEOUT_DEFAULT;
        /**
         * 是否是只读事务
         */
        private boolean readOnly = false;
    }

    @Data
    public static class HotReload {
        /**
         * 是否启用热重载模式
         */
        private boolean enable = false;
        /**
         * 执行热重载的标识文件。如果存在，这个文件变化就执行热重载。如果不存在，则监听所有class文件变化
         */
        private String watchFile;
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
