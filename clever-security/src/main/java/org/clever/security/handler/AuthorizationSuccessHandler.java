package org.clever.security.handler;

import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.AuthorizationSuccessEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 授权成功处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 20:57 <br/>
 */
public interface AuthorizationSuccessHandler extends Ordered {
    /**
     * 授权成功处理
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param event    授权成功事件
     */
    void onAuthorizationSuccess(HttpServletRequest request, HttpServletResponse response, AuthorizationSuccessEvent event);
}
