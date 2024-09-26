package org.clever.security.login;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.clever.security.model.LoginChannel;
import org.clever.security.model.LoginType;
import org.clever.security.model.request.AbstractLoginReq;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 14:22 <br/>
 */
public abstract class AbstractLoginDataCollect implements LoginDataCollect {
    protected void collectBaseDataByParameter(AbstractLoginReq loginData, HttpServletRequest request) {
        if (loginData.getLoginType() == null) {
            String loginType = request.getParameter(AbstractLoginReq.LOGIN_TYPE_PARAM_NAME);
            if (StringUtils.isNotBlank(loginType)) {
                LoginType loginTypeEnum = LoginType.lookup(loginType);
                if (loginTypeEnum != null) {
                    loginData.setLoginType(loginTypeEnum.getId());
                }
            }
        }
        if (loginData.getLoginChannel() == null) {
            String loginChannel = request.getParameter(AbstractLoginReq.LOGIN_CHANNEL_PARAM_NAME);
            if (StringUtils.isNotBlank(loginChannel)) {
                LoginChannel loginChannelEnum = LoginChannel.lookup(loginChannel);
                if (loginChannelEnum != null) {
                    loginData.setLoginChannel(loginChannelEnum.getId());
                }
            }
        }
    }
}
