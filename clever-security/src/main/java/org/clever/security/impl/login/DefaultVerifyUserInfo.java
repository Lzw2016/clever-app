package org.clever.security.impl.login;

import org.clever.core.DateUtils;
import org.clever.core.OrderIncrement;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.AesKeyConfig;
import org.clever.security.config.LoginConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.crypto.PasswordEncoder;
import org.clever.security.exception.*;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.impl.model.request.NamePasswordLoginReq;
import org.clever.security.login.VerifyUserInfo;
import org.clever.security.model.UserInfo;
import org.clever.security.model.request.AbstractLoginReq;
import org.clever.security.utils.AesUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 13:01 <br/>
 */
public class DefaultVerifyUserInfo implements VerifyUserInfo {
    private final PasswordEncoder passwordEncoder;

    public DefaultVerifyUserInfo(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isSupported(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq, UserInfo userInfo) {
        return true;
    }

    @Override
    public void verify(SecurityConfig securityConfig, HttpServletRequest request, AbstractLoginReq loginReq, UserInfo userInfo) throws LoginException {
        if (loginReq == null) {
            throw new LoginDataValidateException("登录数据为空");
        }
        LoginConfig loginConfig = securityConfig.getLogin();
        AesKeyConfig reqAesKey = securityConfig.getReqAesKey();
        SysUser user = (SysUser) request.getAttribute(EnumConstant.REQUEST_ATTR_USER);
        // 登录用户不存在 | 密码错误
        verifyUserInfo(loginConfig, reqAesKey, loginReq, userInfo, user);
        // 用户过期错误 | 用户禁用错误
        verifyUserStatus(loginConfig, user);
        // 登录数量超过最大并发数量错误
        verifyConcurrentLoginCount(loginConfig, userInfo);
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }

    /**
     * 验证用户信息
     */
    protected void verifyUserInfo(LoginConfig loginConfig, AesKeyConfig reqAesKey, AbstractLoginReq loginReq, UserInfo userInfo, SysUser use) {
        // 登录用户不存在
        if (userInfo == null || use == null) {
            if (loginConfig.isHideUserNotFoundException()) {
                throw new BadCredentialsException("用户名或密码错误");
            } else {
                throw new LoginNameNotFoundException("登录名不存在");
            }
        }
        // 密码错误
        if (loginReq instanceof NamePasswordLoginReq) {
            NamePasswordLoginReq namePasswordLoginReq = (NamePasswordLoginReq) loginReq;
            String reqPassword = namePasswordLoginReq.getPassword();
            if (reqAesKey.isEnable()) {
                // 解密密码(请求密码加密在客户端)
                try {
                    reqPassword = AesUtils.decode(reqAesKey.getReqPasswordAesKey(), reqAesKey.getReqPasswordAesIv(), reqPassword);
                } catch (Exception e) {
                    throw new BadCredentialsException(loginConfig.isHideUserNotFoundException() ? "用户名或密码错误" : "登录密码错误", e);
                }
            }
            // 验证密码
            if (!passwordEncoder.matches(reqPassword, use.getPassword())) {
                throw new BadCredentialsException(loginConfig.isHideUserNotFoundException() ? "用户名或密码错误" : "登录密码错误");
            }
        }
    }

    /**
     * 验证用户状态
     */
    protected void verifyUserStatus(LoginConfig loginConfig, SysUser user) {
        // 获取用户信息
        if (user == null) {
            if (loginConfig.isHideUserNotFoundException()) {
                throw new BadCredentialsException("用户名或密码错误");
            } else {
                throw new LoginNameNotFoundException("登录名不存在");
            }
        }
        if (!Objects.equals(user.getIsEnable(), EnumConstant.ENABLED_1)) {
            throw new UserDisabledException("登录账号被禁用");
        }
    }

    /**
     * 登录数量超过最大并发数量校验
     */
    protected void verifyConcurrentLoginCount(LoginConfig loginConfig, UserInfo userInfo) {
        if (loginConfig.getConcurrentLoginCount() <= 0) {
            return;
        }
        if (loginConfig.isAllowAfterLogin()) {
            return;
        }
        // 获取当前用户并发登录数量
        final Date now = new Date();
        long concurrentLoginCount = SecurityDataSource.getQueryDSL().select(sysJwtToken)
                .from(sysJwtToken)
                .where(sysJwtToken.expiredTime.isNull().or(sysJwtToken.expiredTime.gt(DateUtils.toTimestamp(now))))
                .where(sysJwtToken.disable.eq(EnumConstant.ENABLED_0))
                .where(sysJwtToken.userId.eq(userInfo.getUserId()))
                .fetchCount();
        if (concurrentLoginCount >= loginConfig.getConcurrentLoginCount()) {
            throw new ConcurrentLoginException("当前用户并发登录次数达到上限");
        }
    }
}
