package org.clever.js.api.require;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Assert;
import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.module.Module;
import org.clever.js.api.module.*;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/17 09:01 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
@Slf4j
public abstract class AbstractRequire<E, T> implements Require<T> {
    public static final String JS_FILE = ".js";
    public static final String JSON_FILE = ".json";
    /**
     * 文件可省略的后缀名
     */
    private static final String[] FILE_SUFFIX = new String[]{"", JS_FILE, JSON_FILE};
    /**
     * 当前线程缓存(fullPath -> script引擎对象)
     */
    private static final ThreadLocal<Map<String, Object>> REF_CACHE = new ThreadLocal<>();

    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;
    /**
     * 当前模块实例
     */
    private volatile Module<T> currentModule;
    /**
     * 当前模块所属文件夹
     */
    private final Folder currentModuleFolder;

    public AbstractRequire(ScriptEngineContext<E, T> engineContext, Module<T> currentModule, Folder currentModuleFolder) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        Assert.notNull(currentModule, "参数 currentModule 不能为 null");
        Assert.notNull(currentModuleFolder, "参数 currentModuleFolder 不能为 null");
        this.engineContext = engineContext;
        this.currentModule = currentModule;
        this.currentModuleFolder = currentModuleFolder;
    }

    protected AbstractRequire(ScriptEngineContext<E, T> engineContext, Folder currentModuleFolder) {
        Assert.notNull(engineContext, "参数 context 不能为 null");
        Assert.notNull(currentModuleFolder, "参数 currentModuleFolder 不能为 null");
        this.engineContext = engineContext;
        this.currentModuleFolder = currentModuleFolder;
    }

    public Cache<Module<T>> getCache() {
        return engineContext.getModuleCache();
    }

    /**
     * 创建ScriptObject
     */
    protected abstract T newScriptObject();

    /**
     * @param engineContext       引擎上下文
     * @param currentModuleFolder 当前模块所属文件夹
     */
    protected abstract AbstractRequire<E, T> newRequire(ScriptEngineContext<E, T> engineContext, Folder currentModuleFolder);

    /**
     * @param engineContext 引擎上下文
     * @param id            模块的识别符，通常是带有绝对路径的模块文件名
     * @param filename      模块的完全解析后的文件名，带有绝对路径
     * @param exports       表示模块对外输出的值
     * @param parent        返回一个对象，最先引用该模块的模块
     * @param require       module.require() 方法提供了一种加载模块的方法，就像从原始模块调用 require() 一样
     */
    protected abstract Module<T> newModule(ScriptEngineContext<E, T> engineContext, String id, String filename, T exports, Module<T> parent, Require<T> require);

    /**
     * @param function 脚本模块函数
     * @param that     this(当前模块)
     * @param exports  表示模块对外输出的值
     * @param require  require对象，方法提供了一种加载模块的方法
     * @param module   当前模块对象对应的 module 对象
     * @param filename 模块的完全解析后的文件名，带有绝对路径(__filename)
     * @param dirname  模块文件目录(__dirname)
     */
    protected abstract void moduleFunctionCall(T function, T that, T exports, Require<T> require, T module, String filename, String dirname);

    @SuppressWarnings("unchecked")
    @Override
    public T require(final String id) {
        Assert.isNotBlank(id, "参数 id 不能为空");
        Folder moduleFile = loadModuleFolder(id);
        if (moduleFile == null || !moduleFile.isFile()) {
            throw new ModuleNotFoundException("找不到模块id=" + id);
        }
        final String fullPath = moduleFile.getFullPath();
        // 从缓存中加载模块
        Module<T> module = loadModuleForCache(fullPath);
        if (module != null) {
            return module.getExports();
        }
        // 确保每个线程都有自己的refCache
        boolean needRemove = false;
        if (REF_CACHE.get() == null) {
            needRemove = true;
            REF_CACHE.set(new HashMap<>());
        }
        T cached = (T) REF_CACHE.get().get(fullPath);
        if (cached != null) {
            log.debug("# RefCache 命中缓存 -> {}", fullPath);
            return cached;
        } else {
            // 存储对当前加载模块的引用，以避免循环require
            log.debug("# RefCache 加入缓存 -> {}", fullPath);
            REF_CACHE.get().put(fullPath, newScriptObject());
        }
        // 加载 Module
        try {
            module = compileModule(moduleFile);
            module.triggerOnLoaded();
            return module.getExports();
        } catch (Exception e) {
            throw new LoadModuleException("加载ScriptModule失败，id=" + id, e);
        } finally {
            // 需要删除refCache防止内存泄漏
            if (needRemove && REF_CACHE.get() != null) {
                REF_CACHE.remove();
            }
        }
    }

    /**
     * 从缓存中加载模块
     *
     * @param fullPath 全路径
     */
    protected Module<T> loadModuleForCache(String fullPath) {
        Cache<Module<T>> moduleCache = engineContext.getModuleCache();
        Module<T> module = moduleCache.get(fullPath);
        if (module != null) {
            log.debug("# ModuleCache 命中缓存 -> {}", fullPath);
        }
        return module;
    }

    /**
     * 加载或者编译模块
     */
    @SuppressWarnings("unchecked")
    protected Module<T> compileModule(Folder moduleFile) throws Exception {
        final String fullPath = moduleFile.getFullPath();
        CompileModule<T> compileModule = engineContext.getCompileModule();
        // 从缓存中加载模块
        Module<T> module = loadModuleForCache(fullPath);
        if (module != null) {
            return module;
        }
        // 编译加载模块
        String name = moduleFile.getName();
        AbstractRequire<E, T> require = newRequire(engineContext, moduleFile.getParent());
        if (name.endsWith(JSON_FILE)) {
            // json模块
            T exports = compileModule.compileJsonModule(moduleFile);
            module = newModule(engineContext, fullPath, fullPath, exports, currentModule, require);
            require.currentModule = module;
        } else if (name.endsWith(JS_FILE)) {
            // js模块
            T exports = REF_CACHE.get() != null ? (T) REF_CACHE.get().get(fullPath) : null;
            if (exports == null) {
                exports = newScriptObject();
            }
            T function = compileModule.compileScriptModule(moduleFile);
            module = newModule(engineContext, fullPath, fullPath, exports, currentModule, require);
            require.currentModule = module;
            // (function(exports, require, module, __filename, __dirname) {})
            // this         -->
            // exports      --> created.exports
            // require      --> created (Module 对象实现了RequireFunction接口)
            // module       --> created.module
            // __filename   --> filename
            // __dirname    --> dirname
            moduleFunctionCall(function, module.getModule(), module.getExports(), module.getRequire(), module.getModule(), moduleFile.getFullPath(), moduleFile.getParent().getFullPath());
        } else {
            throw new UnSupportModuleException("不支持的ScriptModule类型，name=" + name);
        }
        // 缓存当前加载的 Module
        log.debug("# ModuleCache 加入缓存 -> {}", fullPath);
        engineContext.getModuleCache().put(fullPath, module);
        return module;
    }

    /**
     * 根据加载模块路径，返回加载模块内容(解析模块步骤实现)
     */
    protected Folder loadModuleFolder(final String path) {
        // 判断path是相对路径还是绝对路径
        if (path.startsWith(Folder.ROOT_PATH)
            || path.startsWith(Folder.CURRENT_PATH)
            || path.startsWith(Folder.PARENT_PATH)
            || path.startsWith("\\")
            || path.startsWith(".\\")
            || path.startsWith("..\\")) {
            // 相对路径
            return resolvedModuleFolder(currentModuleFolder, path);
        }
        // 绝对路径
        Folder nodeModules = currentModuleFolder.concat(GlobalConstant.COMMON_JS_NODE_MODULES);
        while (nodeModules != null) {
            if (nodeModules.isDir()) {
                Folder modulePath = resolvedModuleFolder(nodeModules, path);
                if (modulePath != null) {
                    return modulePath;
                }
            }
            // 向上级目录查找
            nodeModules = nodeModules.concat(Folder.PARENT_PATH, Folder.PARENT_PATH, GlobalConstant.COMMON_JS_NODE_MODULES);
        }
        return null;
    }

    /**
     * 解析模块的核心逻辑，解析不到返回null
     * <pre>
     *  参数
     *    path:   .../some/path
     *    module: (module_path)
     *  解析过程
     *    .../some/path/(module_path)
     *    .../some/path/(module_path).js
     *    .../some/path/(module_path)/package.json 中的main属性
     *    .../some/path/(module_path)/index.js
     * </pre>
     *
     * @param dir        当前解析目录
     * @param modulePath 模块路径
     */
    protected Folder resolvedModuleFolder(final Folder dir, final String modulePath) {
        // 直接从文件加载
        for (String suffix : FILE_SUFFIX) {
            Folder moduleFile = dir.concat(modulePath + suffix);
            if (moduleFile != null && moduleFile.isFile()) {
                return moduleFile;
            }
        }
        // 从文件夹加载
        Folder moduleDir = dir.concat(modulePath);
        if (moduleDir != null && moduleDir.isDir()) {
            // 加载package.json中的main模块
            Folder packageJson = moduleDir.concat(GlobalConstant.COMMON_JS_PACKAGE);
            if (packageJson != null && packageJson.isFile()) {
                String packageJsonStr = packageJson.getFileContent();
                if (StringUtils.isNotBlank(packageJsonStr)) {
                    String main = null;
                    try {
                        JSONObject jsonObject = new JSONObject(packageJsonStr);
                        if (jsonObject.has(GlobalConstant.COMMON_JS_PACKAGE_MAIN)) {
                            main = jsonObject.getString(GlobalConstant.COMMON_JS_PACKAGE_MAIN);
                        }
                    } catch (Exception ignored) {
                    }
                    if (StringUtils.isNotBlank(main)) {
                        Folder mainFolder = moduleDir.concat(main);
                        if (mainFolder != null && mainFolder.isFile()) {
                            return mainFolder;
                        }
                    }
                }
            }
            // 加载默认的index.js模块
            Folder indexFolder = moduleDir.concat(GlobalConstant.COMMON_JS_INDEX);
            if (indexFolder != null && indexFolder.isFile()) {
                return indexFolder;
            }
        }
        return null;
    }
}
