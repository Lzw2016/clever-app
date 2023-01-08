package org.clever.web.support.mvc;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppShutdownHook;
import org.clever.core.HotReloadClassLoader;
import org.clever.core.MethodParameter;
import org.clever.core.OrderIncrement;
import org.clever.core.job.DaemonExecutor;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.util.Assert;
import org.clever.web.config.MvcConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/08 19:09 <br/>
 */
public class DefaultHandlerMethodResolver implements HandlerMethodResolver {
    private final HotReloadClassLoader hotReloadClassLoader;

    public DefaultHandlerMethodResolver(MvcConfig.HotReload hotReload) {
        Assert.notNull(hotReload, "参数 hotReload 不能为 null");
        if (hotReload.isEnable()) {
            hotReloadClassLoader = new HotReloadClassLoader(
                    Thread.currentThread().getContextClassLoader(),
//                    new Launcher().getClassLoader(),
                    hotReload.getLocations().toArray(new String[0])
            );
            // 监听 class 文件变化
            DaemonExecutor daemonWatch = new DaemonExecutor("hot-reload-class");
            daemonWatch.scheduleAtFixedRate(this::hotReloadClass, hotReload.getInterval().toMillis());
            AppShutdownHook.addShutdownHook(daemonWatch::stop, OrderIncrement.NORMAL, "停止class热重载");
        } else {
            hotReloadClassLoader = null;
        }
        // TODO
        //ComposeMyBatisMapperSql
        //HotReloadClassLoader
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
        Method method = ReflectionsUtils.getStaticMethod(handlerClass, methodName);
        if (method == null) {
            return null;
        }
        // 创建 HandlerMethod
        MethodParameter[] parameters = new MethodParameter[method.getParameterCount()];
        for (int idx = 0; idx < parameters.length; idx++) {
            parameters[idx] = new MethodParameter(method, idx);
        }
        return new HandlerMethod(reqPath, handlerClass, method, parameters);
    }

    /**
     * 根据 class 全名称加载 class
     *
     * @param className class 全名称
     * @return class不存在返回 null
     */
    protected Class<?> loadClass(final String className, Set<String> excludePackages) throws ClassNotFoundException {
        boolean useHotReload = hotReloadClassLoader != null;
        if (useHotReload && excludePackages != null) {
            for (String excludePackage : excludePackages) {
                if (className.startsWith(excludePackage)) {
                    useHotReload = false;
                    break;
                }
            }
        }
        if (useHotReload) {
            return hotReloadClassLoader.loadClass(className);
        } else {
            return Class.forName(className);
        }
    }

    /**
     * 监听class文件变化
     */
    protected void hotReloadClass() {
        // TODO 监听class文件变化
    }
}
