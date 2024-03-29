package org.clever.security.handler;

import org.clever.core.Ordered;
import org.clever.security.model.jackson2.event.LoginSuccessEvent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:25 <br/>
 */
public interface LoginSuccessHandler extends Ordered {
    /**
     * 登录成功处理逻辑
     *
     * @param request  请求
     * @param response 响应
     * @param event    登录成功事件
     */
    void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, LoginSuccessEvent event) throws IOException, ServletException;
}
