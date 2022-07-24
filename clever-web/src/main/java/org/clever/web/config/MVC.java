package org.clever.web.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MVC配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/24 16:06 <br/>
 */
@Data
public class MVC {
    /**
     * 是否将 TRACE 请求分派到 FrameworkServlet doService 方法。
     */
    private boolean dispatchTraceRequest = false;
    /**
     * 是否将 OPTIONS 请求分派到 FrameworkServlet doService 方法。
     */
    private boolean dispatchOptionsRequest = true;
    /**
     * 在重定向场景中是否应忽略“默认”模型的内容。
     */
    private boolean ignoreDefaultModelOnRedirect = true;
    /**
     * 是否在每个请求结束时发布 ServletRequestHandledEvent。
     */
    private boolean publishRequestHandledEvents = true;
    /**
     * 如果没有找到处理请求的处理程序，是否应该抛出“NoHandlerFoundException”。
     */
    private boolean throwExceptionIfNoHandlerFound = false;
    /**
     * 是否允许在 DEBUG 和 TRACE 级别记录（潜在敏感的）请求详细信息。
     */
    private boolean logRequestDetails;
    /**
     * 是否启用由“HandlerExceptionResolver”解决的异常的警告日志记录，“DefaultHandlerExceptionResolver”除外。
     */
    private boolean logResolvedException = false;
    /**
     * 用于静态资源的路径模式。
     */
    private String staticPathPattern = "/**";
    /**
     * Spring MVC 视图前缀。
     */
    private String prefix;
    /**
     * Spring MVC 视图后缀。
     */
    private String suffix;

    /**
     * 是否启用热重载模式
     */
    private boolean hotReload = false;
    /**
     * MVC处理程序
     */
    private List<Handler> handlers = new ArrayList<>();

    @Data
    public static class Handler {
        /**
         * 接口前缀
         */
        private String apiPrefix = "";
        /**
         * MVC中固定使用Class的包(不支持热重载的包)
         */
        private Set<String> mvcFixedPackages = Collections.emptySet();
        /**
         * 允许MVC调用的package前缀
         */
        private Set<String> mvcAllowPackages = Collections.emptySet();
        /**
         * 热重载代码位置
         */
        private List<String> srcLocations = Collections.singletonList("./src/main/groovy");
    }
}
