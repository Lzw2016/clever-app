package org.clever.security.login;

import org.apache.commons.lang3.StringUtils;
import org.clever.security.model.LoginChannel;
import org.clever.security.model.LoginType;
import org.clever.security.model.request.AbstractUserLoginReq;

import javax.servlet.http.HttpServletRequest;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 14:22 <br/>
 */
public abstract class AbstractLoginDataCollect implements LoginDataCollect {
    protected void collectBaseDataByParameter(AbstractUserLoginReq loginData, HttpServletRequest request) {
        if (loginData.getLoginChannel() == null) {
            String loginChannel = request.getParameter(AbstractUserLoginReq.LOGIN_CHANNEL_PARAM_NAME);
            if (StringUtils.isNotBlank(loginChannel)) {
                LoginChannel loginChannelEnum = LoginChannel.lookup(loginChannel);
                if (loginChannelEnum != null) {
                    loginData.setLoginChannel(String.valueOf(loginChannelEnum.getId()));
                }
            }
        }
    }

    protected LoginType getLoginType(HttpServletRequest request) {
        String loginType = request.getParameter(AbstractUserLoginReq.LOGIN_TYPE_PARAM_NAME);
        return LoginType.lookup(loginType);
    }
}
