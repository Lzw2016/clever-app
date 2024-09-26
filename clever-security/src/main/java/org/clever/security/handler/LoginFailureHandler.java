package org.clever.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.LoginFailureEvent;

import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:22 <br/>
 */
public interface LoginFailureHandler extends Ordered {
    /**
     * 登录失败处理逻辑
     *
     * @param request  请求
     * @param response 响应
     * @param event    登录失败事件
     */
    void onLoginFailure(HttpServletRequest request, HttpServletResponse response, LoginFailureEvent event) throws IOException, ServletException;
}
