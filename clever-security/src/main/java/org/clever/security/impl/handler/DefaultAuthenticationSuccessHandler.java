package org.clever.security.impl.handler;

import org.clever.core.OrderIncrement;
import org.clever.security.handler.AuthenticationSuccessHandler;
import org.clever.security.model.jackson2.event.AuthenticationSuccessEvent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
