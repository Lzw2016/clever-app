package org.clever.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.LogoutFailureEvent;

import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:30 <br/>
 */
public interface LogoutFailureHandler extends Ordered {
    /**
     * 登出失败处理逻辑
     *
     * @param request  请求
     * @param response 响应
     * @param event    登出失败事件
     */
    void onLogoutFailure(HttpServletRequest request, HttpServletResponse response, LogoutFailureEvent event) throws IOException, ServletException;
}
