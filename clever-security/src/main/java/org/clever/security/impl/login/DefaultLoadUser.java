package org.clever.security.impl.login;

import org.clever.core.OrderIncrement;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.QueryDSL;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.UnsupportedLoginTypeException;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.impl.model.request.NamePasswordLoginReq;
import org.clever.security.impl.utils.UserInfoConvertUtils;
import org.clever.security.login.LoadUser;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractLoginReq;

import javax.servlet.http.HttpServletRequest;

import static org.clever.security.impl.model.query.QSysUser.sysUser;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 12:51 <br/>
 */
public class DefaultLoadUser implements LoadUser {
    @Override
    public boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq) {
        return true;
    }

    @Override
    public UserInfo loadUserInfo(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq) {
        if (loginReq instanceof NamePasswordLoginReq) {
            return loadUser(request, (NamePasswordLoginReq) loginReq);
        } else {
            throw new UnsupportedLoginTypeException("不支持的登录类型");
        }
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }

    protected UserInfo loadUser(HttpServletRequest request, NamePasswordLoginReq namePasswordLoginReq) {
        // 根据“loginName”加载用户信息
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        SysUser user = queryDSL.select(sysUser)
                .from(sysUser)
                .where(sysUser.loginName.eq(namePasswordLoginReq.getLoginName()))
                .fetchOne();
        if (user == null) {
            return null;
        }
        request.setAttribute(EnumConstant.REQUEST_ATTR_USER, user);
        UserInfo userInfo = UserInfoConvertUtils.convertToUserInfo(user);
        userInfo.getExt().putAll(BeanCopyUtils.toMap(user));
        userInfo.getExt().remove("id");
        userInfo.getExt().remove("userName");
        userInfo.getExt().remove("password");
        userInfo.getExt().remove("createBy");
        userInfo.getExt().remove("createAt");
        userInfo.getExt().remove("updateBy");
        userInfo.getExt().remove("updateAt");
        return userInfo;
    }
}
