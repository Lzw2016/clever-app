package org.clever.security.impl.login;

import jakarta.servlet.http.HttpServletRequest;
import org.clever.core.OrderIncrement;
import org.clever.core.validator.BaseValidatorUtils;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.LoginDataValidateException;
import org.clever.security.exception.LoginException;
import org.clever.security.login.VerifyLoginData;
import org.clever.security.model.request.AbstractLoginReq;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 12:59 <br/>
 */
public class DefaultVerifyLoginData implements VerifyLoginData {
    @Override
    public boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq) {
        return true;
    }

    @Override
    public void verify(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq) throws LoginException {
        if (loginReq == null) {
            throw new LoginDataValidateException("登录数据为空");
        }
        // 登录数据格式校验(空、长度等)
        verifyLoginData(loginReq);
        // LoginConfig loginConfig = securityConfig.getLogin();
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }

    /**
     * 登录数据格式校验(空、长度等)
     */
    protected void verifyLoginData(AbstractLoginReq loginReq) {
        try {
            BaseValidatorUtils.validateThrowException(loginReq);
        } catch (Exception e) {
            throw new LoginDataValidateException("登录数据校验失败", e);
        }
    }
}
