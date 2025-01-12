package org.clever.security.impl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.OrderIncrement;
import org.clever.core.http.HttpServletRequestUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.data.jdbc.QueryDSL;
import org.clever.security.SecurityDataSource;
import org.clever.security.handler.LoginFailureHandler;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysLoginFailedCount;
import org.clever.security.impl.model.entity.SysLoginLog;
import org.clever.security.model.LoginChannel;
import org.clever.security.model.UserInfo;
import org.clever.security.model.jackson2.event.LoginFailureEvent;
import org.clever.security.model.request.AbstractLoginReq;

import java.util.Date;

import static org.clever.security.impl.model.query.QSysLoginFailedCount.sysLoginFailedCount;
import static org.clever.security.impl.model.query.QSysLoginLog.sysLoginLog;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/01 22:28 <br/>
 */
@Slf4j
public class DefaultLoginFailureHandler implements LoginFailureHandler {
    @Override
    public void onLoginFailure(HttpServletRequest request, HttpServletResponse response, LoginFailureEvent event) {
        UserInfo userInfo = event.getUserInfo();
        if (userInfo == null) {
            return;
        }
        SecurityDataSource.getQueryDSL().beginTX(status -> {
            // 记录登录失败日志
            addUserLoginLog(request, userInfo, event);
            // 增加连续登录失败次数
            addLoginFailedCount(userInfo, event);
            return null;
        });
    }

    @SuppressWarnings("DuplicatedCode")
    protected void addUserLoginLog(HttpServletRequest request, UserInfo userInfo, LoginFailureEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Date now = new Date();
        // 记录登录失败日志user_login_log
        AbstractLoginReq loginData = event.getLoginData();
        if (loginData == null) {
            return;
        }
        SysLoginLog loginLog = new SysLoginLog();
        // loginLog.setId(SnowFlake.SNOW_FLAKE.nextId());
        loginLog.setId(queryDSL.nextId(sysLoginLog.getTableName()));
        loginLog.setUserId(userInfo.getUserId());
        loginLog.setLoginTime(now);
        loginLog.setLoginIp(HttpServletRequestUtils.getIpAddress(request));
        LoginChannel loginChannel = LoginChannel.lookup(loginData.getLoginChannel());
        if (loginChannel == null) {
            loginChannel = LoginChannel.Unknown;
        }
        loginLog.setLoginChannel(loginChannel.getId());
        loginLog.setLoginType(loginData.getLoginType());
        loginLog.setLoginState(EnumConstant.USER_LOGIN_LOG_LOGIN_STATE_0);
        loginLog.setRequestData(JacksonMapper.getInstance().toJson(loginData));
        loginLog.setCreateAt(now);
        queryDSL.insert(sysLoginLog).populate(loginLog).execute();
        log.debug("### 登录失败 -> loginTime={} | loginIp={}", loginLog.getLoginTime(), loginLog.getLoginIp());
    }

    protected void addLoginFailedCount(UserInfo userInfo, LoginFailureEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        AbstractLoginReq loginData = event.getLoginData();
        if (loginData == null) {
            return;
        }
        final Date now = new Date();
        SysLoginFailedCount loginFailedCount = queryDSL.selectFrom(sysLoginFailedCount)
            .where(sysLoginFailedCount.deleteFlag.eq(EnumConstant.LOGIN_FAILED_COUNT_DELETE_FLAG_0))
            .where(sysLoginFailedCount.userId.eq(userInfo.getUserId()))
            .where(sysLoginFailedCount.loginType.eq(loginData.getLoginType()))
            .fetchFirst();
        int failedCount = 1;
        if (loginFailedCount == null) {
            // 新增数据
            loginFailedCount = new SysLoginFailedCount();
            // loginFailedCount.setId(SnowFlake.SNOW_FLAKE.nextId());
            loginFailedCount.setId(queryDSL.nextId(sysLoginFailedCount.getTableName()));
            loginFailedCount.setUserId(userInfo.getUserId());
            loginFailedCount.setLoginType(loginData.getLoginType());
            loginFailedCount.setFailedCount(1);
            loginFailedCount.setLastLoginTime(now);
            loginFailedCount.setDeleteFlag(EnumConstant.LOGIN_FAILED_COUNT_DELETE_FLAG_0);
            loginFailedCount.setCreateAt(now);
            SecurityDataSource.getQueryDSL().insert(sysLoginFailedCount).populate(loginFailedCount).execute();
        } else {
            // 更新数据
            queryDSL.update(sysLoginFailedCount).
                set(sysLoginFailedCount.failedCount, sysLoginFailedCount.failedCount.add(1))
                .set(sysLoginFailedCount.lastLoginTime, now)
                .set(sysLoginFailedCount.updateAt, now)
                .where(sysLoginFailedCount.id.eq(loginFailedCount.getId()))
                .execute();
            failedCount = loginFailedCount.getFailedCount() + 1;
        }
        log.debug("### 增加用户连续登录失败次数: {} | userId=[{}]", failedCount, userInfo.getUserId());
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }
}
