package org.clever.security.utils;

import org.clever.core.exception.BusinessException;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.model.response.ErrorResponse;
import org.clever.security.exception.AuthenticationException;
import org.clever.security.exception.AuthorizationException;
import org.clever.security.exception.LoginException;
import org.clever.security.exception.LogoutException;
import org.clever.web.http.HttpStatus;
import org.clever.web.http.MediaType;
import org.clever.web.utils.GlobalExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/02 20:42 <br/>
 */
public class HttpServletResponseUtils {
    public static HttpRespondHandler HTTP_RESPOND_HANDLER = new HttpRespondHandler() {
        @Override
        public void sendData(HttpServletResponse response, Object data, HttpStatus httpStatus) throws IOException {
            if (response.isCommitted()) {
                return;
            }
            response.setStatus(httpStatus.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            if (data != null) {
                response.getWriter().print(JacksonMapper.getInstance().toJson(data));
                response.getWriter().flush();
            }
        }

        @Override
        public void sendData(HttpServletRequest request, HttpServletResponse response, HttpStatus httpStatus, Throwable e) throws IOException {
            if (response.isCommitted()) {
                return;
            }
            ErrorResponse res = GlobalExceptionHandler.newErrorResponse(request, e);
            if (e instanceof AuthenticationException
                    || e instanceof AuthorizationException
                    || e instanceof LoginException
                    || e instanceof LogoutException
                    || e instanceof BusinessException) {
                res.setMessage(e.getMessage());
            } else {
                res.setMessage("服务器内部错误");
            }
            response.setStatus(httpStatus.value());
            res.setStatus(httpStatus.value());
            // noinspection deprecation
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.getWriter().print(JacksonMapper.getInstance().toJson(new HashMap<>()));
            response.getWriter().flush();
        }
    };

    /**
     * 发送数据到客户端
     *
     * @param response   响应对象
     * @param data       响应数据
     * @param httpStatus http状态
     */
    public static void sendData(HttpServletResponse response, Object data, HttpStatus httpStatus) throws IOException {
        HTTP_RESPOND_HANDLER.sendData(response, data, httpStatus);
    }

    /**
     * 发送数据到客户端(200状态码)
     *
     * @param response 响应对象
     * @param data     响应数据
     */
    public static void sendData(HttpServletResponse response, Object data) throws IOException {
        HTTP_RESPOND_HANDLER.sendData(response, data);
    }

    /**
     * 发送异常到客户端
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param httpStatus 响应状态
     * @param e          异常信息
     */
    public static void sendData(HttpServletRequest request, HttpServletResponse response, HttpStatus httpStatus, Throwable e) throws IOException {
        HTTP_RESPOND_HANDLER.sendData(request, response, httpStatus, e);
    }

    /**
     * 重定向到指定地址
     *
     * @param response 响应对象
     * @param location 重定向地址
     */
    public static void redirect(HttpServletResponse response, String location) throws IOException {
        HTTP_RESPOND_HANDLER.redirect(response, location);
    }

    /**
     * 根据异常获取对应的Http状态码
     */
    public static HttpStatus getHttpStatus(Throwable e) {
        return HTTP_RESPOND_HANDLER.getHttpStatus(e);
    }
}
