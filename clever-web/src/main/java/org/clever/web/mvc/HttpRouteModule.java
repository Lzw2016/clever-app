package org.clever.web.mvc;

/**
 * 注册MVC路由信息的接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/06/10 21:24 <br/>
 */
public interface HttpRouteModule {
    /**
     * 注册MVC路由信息
     *
     * @param registry MVC路由注册器
     */
    void doRegister(HttpRouteRegistry registry);
}
