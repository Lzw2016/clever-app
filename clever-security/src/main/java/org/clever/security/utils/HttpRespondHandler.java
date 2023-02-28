package org.clever.security.utils;

import org.clever.core.exception.BusinessException;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.model.response.ErrorResponse;
import org.clever.security.exception.*;
import org.clever.security.impl.model.response.ForbiddenAccessRes;
import org.clever.security.impl.model.response.LoginRes;
import org.clever.security.impl.model.response.LogoutRes;
import org.clever.security.model.AuthorizationContext;
import org.clever.security.model.LoginContext;
import org.clever.security.model.LogoutContext;
import org.clever.security.model.UserInfo;
import org.clever.web.http.HttpStatus;
import org.clever.web.http.MediaType;
import org.clever.web.utils.GlobalExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 返回响应数据工具
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/27 17:21 <br/>
 */
public abstract class HttpRespondHandler {
    /**
     * 根据异常获取对应的Http状态码
     */
    public HttpStatus getHttpStatus(Throwable e) {
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

    /**
     * 发送数据到客户端
     *
     * @param response   响应对象
     * @param data       响应数据
     * @param httpStatus http状态
     */
    public abstract void sendData(HttpServletResponse response, Object data, HttpStatus httpStatus) throws IOException;

    /**
     * 发送数据到客户端(200状态码)
     *
     * @param response 响应对象
     * @param data     响应数据
     */
    public void sendData(HttpServletResponse response, Object data) throws IOException {
        sendData(response, data, HttpStatus.OK);
    }

    /**
     * 发送异常到客户端
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param e          异常信息
     * @param httpStatus 响应状态
     */
    public abstract void sendData(HttpServletRequest request, HttpServletResponse response, Throwable e, HttpStatus httpStatus) throws IOException;

    /**
     * 发送异常到客户端
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param e        异常信息
     */
    public void sendData(HttpServletRequest request, HttpServletResponse response, Throwable e) throws IOException {
        sendData(request, response, e, this.getHttpStatus(e));
    }

    /**
     * 重定向到指定地址
     *
     * @param response 响应对象
     * @param location 重定向地址
     */
    public void redirect(HttpServletResponse response, String location) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.sendRedirect(location);
    }

    /**
     * 登录成功处理
     *
     * @param context 登录上下文
     */
    public abstract void loginSuccess(LoginContext context) throws IOException;

    /**
     * 登录失败处理
     *
     * @param context 登录上下文
     */
    public abstract void loginFailure(LoginContext context) throws IOException;

    /**
     * 登出成功处理
     *
     * @param context 登出上下文
     */
    public abstract void logoutSuccess(LogoutContext context) throws IOException;

    /**
     * 登出失败处理
     *
     * @param context 登出上下文
     */
    public abstract void logoutFailure(LogoutContext context) throws IOException;

    /**
     * 授权失败处理
     *
     * @param context 授权上下文
     */
    public abstract void forbiddenAccess(AuthorizationContext context) throws IOException;

    /**
     * 可修改的实例
     */
    public static HttpRespondHandler INSTANCE = new HttpRespondHandler() {
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
        public void sendData(HttpServletRequest request, HttpServletResponse response, Throwable e, HttpStatus httpStatus) throws IOException {
            if (response.isCommitted()) {
                return;
            }
            ErrorResponse res = GlobalExceptionHandler.newErrorResponse(request, e);
            if (e instanceof AuthenticationException || e instanceof AuthorizationException || e instanceof LoginException || e instanceof LogoutException || e instanceof BusinessException) {
                res.setMessage(e.getMessage());
            } else {
                res.setMessage("服务器内部错误");
            }
            response.setStatus(httpStatus.value());
            res.setStatus(httpStatus.value());
            // noinspection deprecation
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.getWriter().print(JacksonMapper.getInstance().toJson(res));
            response.getWriter().flush();
        }

        @Override
        public void loginSuccess(LoginContext context) throws IOException {
            UserInfo userInfo = context.getUserInfo().copy();
            LoginRes loginRes = LoginRes.loginSuccess(userInfo, context.getJwtToken(), context.getRefreshToken());
            this.sendData(context.getResponse(), loginRes, HttpStatus.OK);
        }

        @Override
        public void loginFailure(LoginContext context) throws IOException {
            LoginRes loginRes = LoginRes.loginFailure(context.getLoginException().getMessage());
            HttpStatus httpStatus = (context.getLoginException() instanceof RepeatLoginException) ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
            this.sendData(context.getResponse(), loginRes, httpStatus);
        }

        @Override
        public void logoutSuccess(LogoutContext context) throws IOException {
            LogoutRes loginRes = LogoutRes.logoutSuccess(context.getSecurityContext().getUserInfo());
            this.sendData(context.getResponse(), loginRes);
        }

        @Override
        public void logoutFailure(LogoutContext context) throws IOException {
            LogoutRes loginRes = LogoutRes.logoutFailure(context.getLogoutException().getMessage());
            this.sendData(context.getResponse(), loginRes);
        }

        @Override
        public void forbiddenAccess(AuthorizationContext context) throws IOException {
            ForbiddenAccessRes forbiddenAccessRes = new ForbiddenAccessRes("未授权，禁止访问");
            this.sendData(context.getResponse(), forbiddenAccessRes, HttpStatus.FORBIDDEN);
        }
    };
}
