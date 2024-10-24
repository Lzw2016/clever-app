package org.clever.web.mvc;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.BannerUtils;
import org.clever.core.http.HttpServletRequestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/10 21:38 <br/>
 */
@Slf4j
public class HttpRouteRegistry {
    /**
     * 注册的路由信息 {@code ConcurrentMap<routeInfo.getRouteKey(), routeInfo>}
     */
    private final ConcurrentMap<String, RouteInfo> routeInfoMap = new ConcurrentHashMap<>();

    /**
     * 注册MVC路由
     *
     * @param path       请求路径
     * @param httpMethod 请求Method
     * @param clazz      class类全名
     * @param method     class函数名
     */
    public HttpRouteRegistry register(String path, HttpMethod httpMethod, String clazz, String method) {
        Assert.hasText(path, "参数 path 不能为空");
        Assert.notNull(httpMethod, "参数 httpMethod 不能为null");
        Assert.hasText(clazz, "参数 clazz 不能为空");
        Assert.hasText(method, "参数 method 不能为空");
        RouteInfo routeInfo = new RouteInfo(path, httpMethod, clazz, method);
        RouteInfo old = routeInfoMap.get(routeInfo.getKey());
        if (old != null) {
            log.warn("Route被替换 | {} -> {}", old, routeInfo);
        }
        routeInfoMap.put(routeInfo.getKey(), routeInfo);
        return this;
    }

    /**
     * 注册MVC路由
     *
     * @param path       请求路径
     * @param httpMethod 请求Method
     * @param clazz      class类
     * @param method     class函数名
     */
    public HttpRouteRegistry register(String path, HttpMethod httpMethod, Class<?> clazz, String method) {
        Assert.notNull(clazz, "参数 clazz 不能为null");
        return register(path, httpMethod, clazz.getName(), method);
    }

    /**
     * 取消注册MVC路由
     *
     * @param path       请求路径
     * @param httpMethod 请求Method
     */
    public HttpRouteRegistry unregister(String path, String httpMethod) {
        routeInfoMap.remove(RouteInfo.getRouteKey(path, httpMethod));
        return this;
    }

    /**
     * 根据请求信息匹配MVC路由信息
     *
     * @param path       请求路径
     * @param httpMethod 请求Method
     * @return 未匹配返回 null
     */
    public RouteInfo match(String path, String httpMethod) {
        return routeInfoMap.get(RouteInfo.getRouteKey(path, httpMethod));
    }

    /**
     * 根据请求信息匹配MVC路由信息
     *
     * @param request 请求对象
     * @return 未匹配返回 null
     */
    public RouteInfo match(HttpServletRequest request) {
        return match(HttpServletRequestUtils.getPathWithoutContextPath(request), request.getMethod());
    }

    /**
     * 获取所有的路由信息
     */
    public Collection<RouteInfo> getAllRouteInfo() {
        return routeInfoMap.values();
    }

    /**
     * 打印所有路由信息
     */
    public void printAllRouteInfo() {
        List<RouteInfo> routeInfoList = routeInfoMap.values().stream()
            .sorted(Comparator.comparing(RouteInfo::getKey))
            .toList();
        final int httpMethodWidth = 9;
        int pathWidth = 0;
        for (RouteInfo routeInfo : routeInfoList) {
            if (routeInfo.getPath().length() > pathWidth) {
                pathWidth = routeInfo.getPath().length();
            }
        }
        List<String> logs = new ArrayList<>(routeInfoList.size());
        for (RouteInfo routeInfo : routeInfoList) {
            String log = StringUtils.join(
                StringUtils.rightPad("[" + routeInfo.getHttpMethod().name() + "]", httpMethodWidth),
                " " + StringUtils.rightPad(routeInfo.getPath(), pathWidth),
                " -> " + routeInfo.getClazz() + "#" + routeInfo.getMethod()
            );
            logs.add(log);
        }
        BannerUtils.printConfig(log, "RouteInfos", logs.toArray(new String[0]));
    }

    /**
     * 注册get请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry get(String path, String clazz, String method) {
        return register(path, HttpMethod.GET, clazz, method);
    }

    /**
     * 注册get请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry get(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.GET, clazz, method);
    }

    /**
     * 注册post请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry post(String path, String clazz, String method) {
        return register(path, HttpMethod.POST, clazz, method);
    }

    /**
     * 注册post请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry post(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.POST, clazz, method);
    }

    /**
     * 注册put请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry put(String path, String clazz, String method) {
        return register(path, HttpMethod.PUT, clazz, method);
    }

    /**
     * 注册put请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry put(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.PUT, clazz, method);
    }

    /**
     * 注册delete请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry delete(String path, String clazz, String method) {
        return register(path, HttpMethod.DELETE, clazz, method);
    }

    /**
     * 注册delete请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry delete(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.DELETE, clazz, method);
    }

    /**
     * 注册patch请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry patch(String path, String clazz, String method) {
        return register(path, HttpMethod.PATCH, clazz, method);
    }

    /**
     * 注册patch请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry patch(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.PATCH, clazz, method);
    }

    /**
     * 注册options请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry options(String path, String clazz, String method) {
        return register(path, HttpMethod.OPTIONS, clazz, method);
    }

    /**
     * 注册options请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry options(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.OPTIONS, clazz, method);
    }

    /**
     * 注册head请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry head(String path, String clazz, String method) {
        return register(path, HttpMethod.HEAD, clazz, method);
    }

    /**
     * 注册head请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry head(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.HEAD, clazz, method);
    }

    /**
     * 注册trace请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry trace(String path, String clazz, String method) {
        return register(path, HttpMethod.TRACE, clazz, method);
    }

    /**
     * 注册trace请求MVC路由
     *
     * @param path   请求路径
     * @param clazz  class类
     * @param method class函数名
     */
    public HttpRouteRegistry trace(String path, Class<?> clazz, String method) {
        return register(path, HttpMethod.TRACE, clazz, method);
    }

    /**
     * 开始基于class注册MVC路由
     *
     * @param clazz    class类
     * @param basePath 基础路径
     */
    public ClassRegisterToRoute startClass(String clazz, String basePath) {
        return new ClassRegisterToRoute(this, basePath, clazz);
    }

    /**
     * 开始基于class注册MVC路由
     *
     * @param clazz    class类
     * @param basePath 基础路径
     */
    public ClassRegisterToRoute startClass(Class<?> clazz, String basePath) {
        Assert.notNull(clazz, "参数 clazz 不能为null");
        return new ClassRegisterToRoute(this, basePath, clazz.getName());
    }

    /**
     * 开始基于class注册MVC路由
     *
     * @param clazz class类
     */
    public ClassRegisterToRoute startClass(String clazz) {
        return startClass(StringUtils.EMPTY, clazz);
    }

    /**
     * 开始基于class注册MVC路由
     *
     * @param clazz class类
     */
    public ClassRegisterToRoute startClass(Class<?> clazz) {
        Assert.notNull(clazz, "参数 clazz 不能为null");
        return startClass(StringUtils.EMPTY, clazz.getName());
    }

    /**
     * 开始基于basePath注册MVC路由
     *
     * @param basePath 基础路径
     */
    public BasePathRegisterToRoute startBasePath(String basePath) {
        return new BasePathRegisterToRoute(this, basePath);
    }

    private static String concatPath(String basePath, String path) {
        basePath = StringUtils.trimToEmpty(basePath);
        path = StringUtils.trimToEmpty(path);
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return basePath + path;
    }

    /**
     * 基于class注册MVC路由
     */
    public static class ClassRegisterToRoute {
        private final HttpRouteRegistry register;
        private final String basePath;
        private final String clazz;

        public ClassRegisterToRoute(HttpRouteRegistry register, String basePath, String clazz) {
            Assert.notNull(register, "参数register不能为null");
            Assert.notNull(basePath, "参数basePath不能为null");
            Assert.hasText(clazz, "参数clazz不能为空");
            this.register = register;
            this.basePath = basePath;
            this.clazz = clazz;
        }

        /**
         * 结束基于class注册MVC路由
         */
        public HttpRouteRegistry endClass() {
            return register;
        }

        public ClassRegisterToRoute register(String path, HttpMethod httpMethod, String method) {
            register.register(HttpRouteRegistry.concatPath(basePath, path), httpMethod, clazz, method);
            return this;
        }

        public ClassRegisterToRoute get(String path, String method) {
            return register(path, HttpMethod.GET, method);
        }

        public ClassRegisterToRoute post(String path, String method) {
            return register(path, HttpMethod.POST, method);
        }

        public ClassRegisterToRoute put(String path, String method) {
            return register(path, HttpMethod.PUT, method);
        }

        public ClassRegisterToRoute delete(String path, String method) {
            return register(path, HttpMethod.DELETE, method);
        }

        public ClassRegisterToRoute patch(String path, String method) {
            return register(path, HttpMethod.PATCH, method);
        }

        public ClassRegisterToRoute options(String path, String method) {
            return register(path, HttpMethod.OPTIONS, method);
        }

        public ClassRegisterToRoute head(String path, String method) {
            return register(path, HttpMethod.HEAD, method);
        }

        public ClassRegisterToRoute trace(String path, String method) {
            return register(path, HttpMethod.TRACE, method);
        }
    }

    /**
     * 基于basePath注册MVC路由
     */
    public static class BasePathRegisterToRoute {
        private final HttpRouteRegistry register;
        private final String basePath;

        public BasePathRegisterToRoute(HttpRouteRegistry register, String basePath) {
            Assert.notNull(register, "参数register不能为null");
            Assert.notNull(basePath, "参数basePath不能为null");
            this.register = register;
            this.basePath = basePath;
        }

        /**
         * 结束基于basePath注册MVC路由
         */
        public HttpRouteRegistry endBasePath() {
            return register;
        }

        public BasePathRegisterToRoute register(String path, HttpMethod httpMethod, String clazz, String method) {
            register.register(basePath + path, httpMethod, clazz, method);
            return this;
        }

        public BasePathRegisterToRoute get(String path, String clazz, String method) {
            return register(path, HttpMethod.GET, clazz, method);
        }

        public BasePathRegisterToRoute post(String path, String clazz, String method) {
            return register(path, HttpMethod.POST, clazz, method);
        }

        public BasePathRegisterToRoute put(String path, String clazz, String method) {
            return register(path, HttpMethod.PUT, clazz, method);
        }

        public BasePathRegisterToRoute delete(String path, String clazz, String method) {
            return register(path, HttpMethod.DELETE, clazz, method);
        }

        public BasePathRegisterToRoute patch(String path, String clazz, String method) {
            return register(path, HttpMethod.PATCH, clazz, method);
        }

        public BasePathRegisterToRoute options(String path, String clazz, String method) {
            return register(path, HttpMethod.OPTIONS, clazz, method);
        }

        public BasePathRegisterToRoute head(String path, String clazz, String method) {
            return register(path, HttpMethod.HEAD, clazz, method);
        }

        public BasePathRegisterToRoute trace(String path, String clazz, String method) {
            return register(path, HttpMethod.TRACE, clazz, method);
        }
    }
}
