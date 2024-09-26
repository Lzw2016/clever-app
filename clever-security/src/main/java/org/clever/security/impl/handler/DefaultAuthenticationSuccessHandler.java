package org.clever.security.impl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.clever.core.OrderIncrement;
import org.clever.security.handler.AuthenticationSuccessHandler;
import org.clever.security.model.jackson2.event.AuthenticationSuccessEvent;

import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/01/21 19:50 <br/>
 */
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, AuthenticationSuccessEvent event) throws IOException, ServletException {
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }
}
