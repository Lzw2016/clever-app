package org.clever.security.impl.utils;

import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.model.UserInfo;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 16:28 <br/>
 */
public class UserInfoConvertUtils {
    public static final String LOGIN_NAME = "loginName";

    public static UserInfo convertToUserInfo(SysUser user) {
        if (user == null) {
            return null;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUserName(user.getUserName());
        userInfo.getExt().put(LOGIN_NAME, user.getLoginName());
        return userInfo;
    }
}
