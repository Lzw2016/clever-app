package org.clever.security.impl.login;

import org.clever.core.Conv;
import org.clever.security.config.TokenConfig;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.login.AddJwtTokenExtData;
import org.clever.security.model.UserInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/01/21 19:54 <br/>
 */
public class DefaultAddJwtTokenExtData implements AddJwtTokenExtData {
    @Override
    public Map<String, Object> addExtData(HttpServletRequest request, TokenConfig tokenConfig, UserInfo userInfo, Map<String, Object> extData) {
        SysUser user = (SysUser) request.getAttribute(EnumConstant.REQUEST_ATTR_USER);
        extData.put(EnumConstant.JWT_EXT_USER_ID, Conv.asString(user.getId()));
        return extData;
    }
}
