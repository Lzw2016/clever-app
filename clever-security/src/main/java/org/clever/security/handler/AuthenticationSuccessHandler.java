package org.clever.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.AuthenticationSuccessEvent;

import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/05 18:43 <br/>
 */
public interface AuthenticationSuccessHandler extends Ordered {
    /**
     * 身份认证成功处理逻辑
     *
     * @param request  请求
     * @param response 响应
     * @param event    身份认证成功事件
     */
    void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, AuthenticationSuccessEvent event) throws IOException, ServletException;
}
