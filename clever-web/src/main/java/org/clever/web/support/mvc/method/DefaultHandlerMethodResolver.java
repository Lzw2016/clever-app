package org.clever.web.support.mvc.method;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.*;
import org.clever.core.job.DaemonExecutor;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.util.Assert;
import org.clever.web.config.MvcConfig;
import org.clever.web.support.mvc.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/08 19:09 <br/>
 */
@Slf4j
public class DefaultHandlerMethodResolver implements HandlerMethodResolver {
    // private static final Method NULL_METHOD =
    @Getter
    protected final String rootPath;
    /**
     * 加载class文件路径 {@code Map<location配置, absolutePath>}
     */
    @Getter
    protected final Map<String, String> locationMap;
    /**
     * 是否使用热重载
     */
    @Getter
    protected final boolean enableHotReload;
    /**
     * 支持读取 Method 原始参数名
     */
    protected final ParameterNameDiscoverer parameterNameDiscoverer;
    /**
     * 支持热重载的 ClassLoader
     */
    @Getter
    protected final HotReloadClassLoader hotReloadClassLoader;
    /**
     * Handler Class 缓存 {@code Map<className, Method>}
     */
    protected final ConcurrentMap<String, Class<?>> handlerClassCache = new ConcurrentHashMap<>();
    /**
     * Handler Method 缓存 {@code Map<className@methodName, Method>}
     */
    protected final ConcurrentMap<String, Method> handlerMethodCache = new ConcurrentHashMap<>();
    /**
     * class文件的最后修改时间搓 {@code ConcurrentMap<AbsolutePath, LastModified>}
     */
    protected final ConcurrentMap<String, Long> classLastModifiedMap = new ConcurrentHashMap<>();

    public DefaultHandlerMethodResolver(String rootPath, MvcConfig.HotReload hotReload) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(hotReload, "参数 hotReload 不能为 null");
        this.rootPath = rootPath;
        this.locationMap = Collections.unmodifiableMap(ResourcePathUtils.getAbsolutePath(rootPath, hotReload.getLocations()));
        if (hotReload.isEnable()) {
            hotReloadClassLoader = new HotReloadClassLoader(
                    this.getClass().getClassLoader(),
                    // Thread.currentThread().getContextClassLoader(),
                    // new Launcher().getClassLoader(),
                    hotReload.getLocations().stream().map(location -> locationMap.getOrDefault(location, location)).toArray(String[]::new)
            );
            // 监听 class 文件变化
            DaemonExecutor daemonWatch = new DaemonExecutor("hot-reload-class");
            daemonWatch.scheduleAtFixedRate(this::hotReloadClass, hotReload.getInterval().toMillis());
            AppShutdownHook.addShutdownHook(daemonWatch::stop, OrderIncrement.NORMAL, "停止class热重载");
            AppContextHolder.registerBean("hotReloadClassLoader", hotReloadClassLoader, true);
        } else {
            hotReloadClassLoader = null;
        }
        this.enableHotReload = hotReloadClassLoader != null;
        this.parameterNameDiscoverer = new DefaultParameterNameDiscoverer(!this.enableHotReload);
    }

    @Override
    public HandlerMethod getHandleMethod(HttpServletRequest request, HttpServletResponse response, MvcConfig mvcConfig) throws Exception {
        final String reqPath = request.getPathInfo();
        // 解析出 Handler Method 的全路径
        String tmpStr = StringUtils.trimToEmpty(mvcConfig.getPackagePrefix()) + reqPath.substring(mvcConfig.getPath().length());
        tmpStr = StringUtils.replaceChars(tmpStr, '/', '.');
        tmpStr = StringUtils.replaceChars(tmpStr, '\\', '.');
        String[] tmpArr = StringUtils.split(tmpStr, '@');
        if (tmpArr.length != 2) {
            return null;
        }
        final String className = tmpArr[0];
        final String methodName = tmpArr[1];
        if (StringUtils.isBlank(className) || StringUtils.isBlank(methodName)) {
            return null;
        }
        // 验证 Handler Method 是否在白名单中
        boolean allowInvoke = false;
        if (mvcConfig.getAllowPackages() != null) {
            for (String allowPackage : mvcConfig.getAllowPackages()) {
                if (className.startsWith(allowPackage)) {
                    allowInvoke = true;
                    break;
                }
            }
        }
        if (!allowInvoke) {
            return null;
        }
        // 加载 handlerClass 以及 method
        final Class<?> handlerClass = loadClass(className, mvcConfig.getHotReload().getExcludePackages());
        if (handlerClass == null) {
            return null;
        }
        final Method handlerMethod = loadMethod(handlerClass, methodName);
        if (handlerMethod == null || !Modifier.isPublic(handlerMethod.getModifiers())) {
            return null;
        }
        // 创建 HandlerMethod
        MethodParameter[] parameters = new MethodParameter[handlerMethod.getParameterCount()];
        for (int idx = 0; idx < parameters.length; idx++) {
            MethodParameter methodParameter = new MethodParameter(handlerMethod, idx);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            parameters[idx] = methodParameter;
        }
        return new HandlerMethod(reqPath, handlerClass, handlerMethod, parameters);
    }

    /**
     * 根据 class 全名称加载 class
     *
     * @param className       class 全名称
     * @param excludePackages 不使用热重载的package前缀
     * @return class不存在返回 null
     */
    protected Class<?> loadClass(final String className, Set<String> excludePackages) {
        boolean useHotReload = enableHotReload;
        if (useHotReload && excludePackages != null) {
            for (String excludePackage : excludePackages) {
                if (className.startsWith(excludePackage)) {
                    useHotReload = false;
                    break;
                }
            }
        }
        Class<?> handlerClass = null;
        try {
            if (useHotReload) {
                // 支持热重载
                handlerClass = hotReloadClassLoader.loadClass(className);
            } else {
                // 不支持热重载
                handlerClass = handlerClassCache.get(className);
                if (handlerClass == null) {
                    handlerClass = Class.forName(className);
                    handlerClassCache.put(className, handlerClass);
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("class不存在: {}", className);
        }
        return handlerClass;
    }

    protected Method loadMethod(Class<?> handlerClass, String methodName) {
        String key = handlerClass.getName() + "@" + methodName;
        Method handlerMethod = null;
        if (!enableHotReload) {
            handlerMethod = handlerMethodCache.get(key);
        }
        if (handlerMethod == null) {
            handlerMethod = ReflectionsUtils.getStaticMethod(handlerClass, methodName);
        }
        if (!enableHotReload && handlerMethod != null) {
            handlerMethodCache.put(key, handlerMethod);
        }
        return handlerMethod;
    }

    /**
     * 监听class文件变化
     */
    protected void hotReloadClass() {
        if (classLastModifiedMap.isEmpty()) {
            classLastModifiedMap.putAll(getAllLastModified());
            return;
        }
        final Map<String, Long> newLastModifiedMap = getAllLastModified();
        final List<String> changedClass = new ArrayList<>(128);
        // 变化的文件(包含删除的文件)
        classLastModifiedMap.forEach((absolutePath, lastModified) -> {
            Long last = newLastModifiedMap.get(absolutePath);
            // if (last == null) {
            //     return;
            // }
            if (!Objects.equals(last, lastModified)) {
                changedClass.add(absolutePath);
            }
        });
        // 新增的文件
        newLastModifiedMap.forEach((absolutePath, lastModified) -> {
            if (!classLastModifiedMap.containsKey(absolutePath)) {
                changedClass.add(absolutePath);
            }
        });
        if (!changedClass.isEmpty()) {
            try {
                // 休眠一下防止抖动(编译时class文件连续的变化)
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            log.info("class文件更新,文件: {}", changedClass);
            final Map<String, Long> latest = getAllLastModified();
            hotReloadClassLoader.unloadAllClass();
            classLastModifiedMap.clear();
            classLastModifiedMap.putAll(latest);
        }
    }

    protected Map<String, Long> getAllLastModified() {
        final Map<String, Long> fileLastModifiedMap = new HashMap<>(classLastModifiedMap.size());
        locationMap.forEach((location, absolutePath) -> {
            File dir = new File(absolutePath);
            if (!dir.isDirectory()) {
                return;
            }
            Collection<File> files = FileUtils.listFiles(dir, new String[]{"class"}, true);
            files.forEach(file -> fileLastModifiedMap.put(file.getAbsolutePath(), file.lastModified()));
        });
        return fileLastModifiedMap;
    }
}
