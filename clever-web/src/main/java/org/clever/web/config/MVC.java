package org.clever.web.config;

import io.javalin.http.HandlerType;
import lombok.Data;

import java.util.Arrays;
import java.util.HashSet;
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
//    /**
//     * 如果没有找到处理请求的处理程序，是否应该抛出“NoHandlerFoundException”。
//     */
//    private boolean throwExceptionIfNoHandlerFound = false;
//    /**
//     * 是否启用由“HandlerExceptionResolver”解决的异常的警告日志记录，“DefaultHandlerExceptionResolver”除外。
//     */
//    private boolean logResolvedException = false;

    /**
     * MVC接口前缀
     */
    private String path = "/api/*";
    /**
     * MVC支持的Http Method
     */
    private Set<HandlerType> httpMethod = new HashSet<HandlerType>() {{
        add(HandlerType.POST);
        add(HandlerType.GET);
        add(HandlerType.PUT);
        add(HandlerType.PATCH);
        add(HandlerType.DELETE);
    }};
    /**
     * 允许MVC调用的package前缀
     */
    private Set<String> allowPackages = new HashSet<>();
    /**
     * 热重载配置
     */
    private HotReload hotReload = new HotReload();

    @Data
    public static class HotReload {
        /**
         * 是否启用热重载模式
         */
        private boolean enable = false;
        /**
         * 固定使用class的包(不支持热重载的包)
         */
        private Set<String> excludePackages = new HashSet<>();
        /**
         * 热重载class位置 TODO 获取系统 classpath 路径
         */
        private List<String> locations = Arrays.asList(
                "./build/classes/java",
                "./build/classes/kotlin",
                "./build/classes/groovy",
                "./out/production/classes"
        );
    }
}
