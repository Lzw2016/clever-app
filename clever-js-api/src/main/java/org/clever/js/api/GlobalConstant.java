package org.clever.js.api;

import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.api.internal.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/17 21:13 <br/>
 */
public interface GlobalConstant {
    /**
     * JS时间默认格式
     */
    String JS_DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // --------------------------------------------------------------------------------------------
    // 全局变量
    // --------------------------------------------------------------------------------------------
    /**
     * require用于加载其他模块
     */
    String ENGINE_REQUIRE = "require";
    /**
     * 共享的全局变量
     */
    String ENGINE_GLOBAL = "global";

    // --------------------------------------------------------------------------------------------
    // module实例成员
    // --------------------------------------------------------------------------------------------
    /**
     * 主模块
     */
    String MODULE_MAIN = "<main>";
    /**
     * 模块的识别符，通常是带有绝对路径的模块文件名
     */
    String MODULE_ID = "id";
    /**
     * 模块的完全解析后的文件名，带有绝对路径
     */
    String MODULE_FILENAME = "filename";
    /**
     * 模块是否已经加载完成，或正在加载中
     */
    String MODULE_LOADED = "loaded";
    /**
     * 返回一个对象，最先引用该模块的模块
     */
    String MODULE_PARENT = "parent";
    /**
     * 模块的搜索路径
     */
    String MODULE_PATHS = "paths";
    /**
     * 被该模块引用的模块对象
     */
    String MODULE_CHILDREN = "children";
    /**
     * 表示模块对外输出的值
     */
    String MODULE_EXPORTS = "exports";
    /**
     * module.require(id) 方法提供了一种加载模块的方法，就像从原始模块调用 require(id) 一样
     */
    String MODULE_REQUIRE = "require";

    // --------------------------------------------------------------------------------------------
    // CommonJS解析模块
    // --------------------------------------------------------------------------------------------
    /**
     * package.json 文件名
     */
    String COMMON_JS_PACKAGE = "package.json";
    /**
     * package.json 的 main 属性
     */
    String COMMON_JS_PACKAGE_MAIN = "main";
    /**
     * index.js 文件名
     */
    String COMMON_JS_INDEX = "index.js";
    /**
     * node_modules 文件夹名
     */
    String COMMON_JS_NODE_MODULES = "node_modules";

    // --------------------------------------------------------------------------------------------
    // 默认允许访问的class和全局对象
    // --------------------------------------------------------------------------------------------

    /**
     * 默认不允许访问的Class
     */
    Set<Class<?>> DEFAULT_DENY_ACCESS_CLASS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                System.class,
                Thread.class
            )
        )
    );

    /**
     * 默认注入的全局对象
     */
    Map<String, Object> DEFAULT_REGISTER_GLOBAL_VARS = Collections.unmodifiableMap(new HashMap<String, Object>() {{
        put("console", LoggerConsole.INSTANCE);
        put("print", LoggerConsole.INSTANCE);
        put("LoggerFactory", LoggerFactory.INSTANCE);
    }});

    /**
     * 自定义注入的全局对象
     */
    Map<String, Object> CUSTOM_REGISTER_GLOBAL_VARS = new ConcurrentHashMap<>(16);
}
