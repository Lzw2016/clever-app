package org.clever.web.mvc;

import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/25 15:59 <br/>
 */
@Data
public class RouteInfo implements Serializable {
    /**
     * 路由路径
     */
    private final String path;
    /**
     * 路由method
     */
    private final HttpMethod httpMethod;
    /**
     * class全路径
     */
    private final String clazz;
    /**
     * class method
     */
    private final String method;

    /**
     * @param path       路由路径
     * @param httpMethod 路由method
     * @param clazz      class全路径
     * @param method     class method
     */
    public RouteInfo(String path, HttpMethod httpMethod, String clazz, String method) {
        Assert.hasText(path, "参数 path 不能为空");
        Assert.notNull(httpMethod, "参数 httpMethod 不能为null");
        Assert.hasText(clazz, "参数 clazz 不能为空");
        Assert.hasText(method, "参数 method 不能为空");
        this.path = path;
        this.httpMethod = httpMethod;
        this.clazz = clazz;
        this.method = method;
    }

    /**
     * 获取路由唯一key
     */
    public String getKey() {
        return getRouteKey(path, httpMethod.name());
    }

    /**
     * 获取路由唯一key
     *
     * @param path       请求路径
     * @param httpMethod 请求Method
     */
    public static String getRouteKey(String path, String httpMethod) {
        return String.format("%s_%s", path, httpMethod);
    }
}
