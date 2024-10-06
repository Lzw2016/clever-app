package org.clever.security.model.jackson2.event;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.clever.security.config.LoginConfig;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractLoginReq;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录成功事件
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 16:14 <br/>
 */
@Data
public class LoginSuccessEvent implements Serializable {
    /**
     * 请求对象
     */
    private final HttpServletRequest request;
    /**
     * 响应对象
     */
    private final HttpServletResponse response;
    /**
     * 用户登录配置
     */
    private final LoginConfig loginConfig;
    /**
     * 用户登录数据
     */
    private final AbstractLoginReq loginData;
    /**
     * 用户信息(从数据库或其它服务加载)
     */
    private final UserInfo userInfo;
    /**
     * JWT-Token
     */
    private final String jwtToken;
    /**
     * JWT-Token对象
     */
    private final Claims claims;
    /**
     * 刷新Token
     */
    private String refreshToken;
    /**
     * 刷新Token过期时间
     */
    private Date rtExpiredTime;

    public LoginSuccessEvent(
            HttpServletRequest request,
            HttpServletResponse response,
            LoginConfig loginConfig,
            AbstractLoginReq loginData,
            UserInfo userInfo,
            String jwtToken,
            Claims claims) {
        this.request = request;
        this.response = response;
        this.loginConfig = loginConfig;
        this.loginData = loginData;
        this.userInfo = userInfo;
        this.jwtToken = jwtToken;
        this.claims = claims;
    }
}
