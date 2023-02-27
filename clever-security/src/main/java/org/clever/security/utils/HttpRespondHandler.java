package org.clever.security.utils;

import org.clever.core.exception.BusinessException;
import org.clever.security.exception.*;
import org.clever.web.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/27 17:21 <br/>
 */
public interface HttpRespondHandler {
    /**
     * 发送数据到客户端
     *
     * @param response   响应对象
     * @param data       响应数据
     * @param httpStatus http状态
     */
    void sendData(HttpServletResponse response, Object data, HttpStatus httpStatus) throws IOException;

    /**
     * 发送数据到客户端(200状态码)
     *
     * @param response 响应对象
     * @param data     响应数据
     */
    default void sendData(HttpServletResponse response, Object data) throws IOException {
        sendData(response, data, HttpStatus.OK);
    }

    /**
     * 发送异常到客户端
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param httpStatus 响应状态
     * @param e          异常信息
     */
    void sendData(HttpServletRequest request, HttpServletResponse response, HttpStatus httpStatus, Throwable e) throws IOException;

    /**
     * 重定向到指定地址
     *
     * @param response 响应对象
     * @param location 重定向地址
     */
    default void redirect(HttpServletResponse response, String location) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.sendRedirect(location);
    }

    /**
     * 根据异常获取对应的Http状态码
     */
    default HttpStatus getHttpStatus(Throwable e) {
        if (e == null) {
            return HttpStatus.OK;
        }
        if (e instanceof AuthenticationInnerException
                || e instanceof AuthorizationInnerException
                || e instanceof LoginInnerException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (e instanceof AuthenticationException
                || e instanceof AuthorizationException
                || e instanceof LoginException
                || e instanceof LogoutException
                || e instanceof BusinessException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
