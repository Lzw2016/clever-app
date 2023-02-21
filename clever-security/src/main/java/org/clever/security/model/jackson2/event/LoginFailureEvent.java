package org.clever.security.model.jackson2.event;

import lombok.Data;
import org.clever.security.exception.LoginException;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractUserLoginReq;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * 登录失败事件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:13 <br/>
 */
@Data
public class LoginFailureEvent implements Serializable {
    /**
     * 请求对象
     */
    private final HttpServletRequest request;
    /**
     * 响应对象
     */
    private final HttpServletResponse response;
    /**
     * 用户登录数据
     */
    private final AbstractUserLoginReq loginData;
    /**
     * 用户信息(从数据库或其它服务加载)
     */
    private final UserInfo userInfo;
    /**
     * 登录异常对象
     */
    private final LoginException loginException;

    public LoginFailureEvent(HttpServletRequest request, HttpServletResponse response, AbstractUserLoginReq loginData, UserInfo userInfo, LoginException loginException) {
        this.request = request;
        this.response = response;
        this.loginData = loginData;
        this.userInfo = userInfo;
        this.loginException = loginException;
    }
}
