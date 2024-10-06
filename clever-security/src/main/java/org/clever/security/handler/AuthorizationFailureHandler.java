package org.clever.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.AuthorizationFailureEvent;

/**
 * 授权失败处理
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 20:56 <br/>
 */
public interface AuthorizationFailureHandler extends Ordered {
    /**
     * 授权失败处理
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param event    授权失败事件
     */
    void onAuthorizationFailure(HttpServletRequest request, HttpServletResponse response, AuthorizationFailureEvent event);
}
