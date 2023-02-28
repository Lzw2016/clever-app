package org.clever.security.impl.login;

import org.clever.core.OrderIncrement;
import org.clever.security.config.LoginConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.impl.model.request.NamePasswordLoginReq;
import org.clever.security.login.AbstractLoginDataCollect;
import org.clever.security.model.request.AbstractLoginReq;
import org.clever.security.utils.HttpServletRequestUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 12:54 <br/>
 */
public class DefaultLoginDataCollect extends AbstractLoginDataCollect {
    @Override
    public boolean isSupported(SecurityConfig securityConfig, String loginPath, HttpServletRequest request) {
        return true;
    }

    @Override
    public AbstractLoginReq collectLoginData(SecurityConfig securityConfig, HttpServletRequest request) {
        return getNamePasswordLoginReq(securityConfig, request);
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }

    protected NamePasswordLoginReq getNamePasswordLoginReq(SecurityConfig securityConfig, HttpServletRequest request) {
        LoginConfig login = securityConfig.getLogin();
        NamePasswordLoginReq req = HttpServletRequestUtils.parseBodyToEntity(request, NamePasswordLoginReq.class);
        if (req == null && login.isPostOnly()) {
            return null;
        }
        if (req == null) {
            req = new NamePasswordLoginReq();
        }
        // 收集基础数据
        collectBaseDataByParameter(req, request);
        // 收集当前登录类型数据
        if (req.getLoginName() == null) {
            String loginName = request.getParameter(NamePasswordLoginReq.LOGIN_NAME_PARAM_NAME);
            if (loginName != null) {
                req.setLoginName(loginName);
            }
        }
        if (req.getPassword() == null) {
            String password = request.getParameter(NamePasswordLoginReq.PASSWORD_PARAM_NAME);
            if (password != null) {
                req.setPassword(password);
            }
        }
        if (req.getLoginName() == null && req.getPassword() == null) {
            return null;
        }
        return req;
    }
}
