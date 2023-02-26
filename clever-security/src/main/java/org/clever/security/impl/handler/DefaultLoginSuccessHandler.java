package org.clever.security.impl.handler;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.OrderIncrement;
import org.clever.core.SystemClock;
import org.clever.core.http.HttpServletRequestUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.LoginConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.handler.LoginSuccessHandler;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysJwtToken;
import org.clever.security.impl.model.entity.SysLoginFailedCount;
import org.clever.security.impl.model.entity.SysLoginLog;
import org.clever.security.impl.model.query.QSysJwtToken;
import org.clever.security.model.LoginChannel;
import org.clever.security.model.UserInfo;
import org.clever.security.model.jackson2.event.LoginSuccessEvent;
import org.clever.security.model.request.AbstractUserLoginReq;
import org.clever.security.utils.SecurityRedisKey;
import org.clever.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;
import static org.clever.security.impl.model.query.QSysLoginFailedCount.sysLoginFailedCount;
import static org.clever.security.impl.model.query.QSysLoginLog.sysLoginLog;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/01 22:31 <br/>
 */
@Slf4j
public class DefaultLoginSuccessHandler implements LoginSuccessHandler {
    private final SecurityConfig securityConfig;

    public DefaultLoginSuccessHandler(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, LoginSuccessEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        // 保存JWT-Token
        SysJwtToken jwtToken = queryDSL.beginTX(status -> {
            // 保存JWT-Token
            SysJwtToken sysJwtToken = addJwtToken(event);
            long jwtTokenId = sysJwtToken.getId();
            // 记录登录成功日志
            addUserLoginLog(jwtTokenId, request, event);
            // 清除连续登录失败次数
            clearLoginFailedCount(event);
            // 挤下最早登录的用户
            disableFirstToken(event);
            return sysJwtToken;
        });
        // SysJwtToken缓存在Redis中
        if (dataSource.isEnableRedis()) {
            String key = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(jwtToken.getUserId()), Conv.asString(jwtToken.getId()));
            SecurityDataSource.getRedis().vSet(key, jwtToken, jwtToken.getExpiredTime().getTime() - SystemClock.now());
        }
    }

    protected SysJwtToken addJwtToken(LoginSuccessEvent event) {
        UserInfo userInfo = event.getUserInfo();
        String jwtToken = event.getJwtToken();
        Claims claims = event.getClaims();
        Assert.notNull(userInfo, "userInfo不能为null");
        Assert.hasText(jwtToken, "jwtToken不能为空");
        Assert.notNull(claims, "claims不能为null");
        SysJwtToken sysJwtToken = new SysJwtToken();
        sysJwtToken.setId(Long.parseLong(claims.getId()));
        sysJwtToken.setUserId(userInfo.getUserId());
        sysJwtToken.setToken(jwtToken);
        sysJwtToken.setExpiredTime(claims.getExpiration());
        sysJwtToken.setDisable(EnumConstant.DISABLE_0);
        sysJwtToken.setRefreshToken(event.getRefreshToken());
        sysJwtToken.setRtExpiredTime(event.getRtExpiredTime());
        sysJwtToken.setRtState(EnumConstant.JWT_TOKEN_REFRESH_TOKEN_STATE_1);
        sysJwtToken.setCreateAt(new Date());
        SecurityDataSource.getQueryDSL().insert(QSysJwtToken.sysJwtToken).populate(sysJwtToken).execute();
        return sysJwtToken;
    }

    @SuppressWarnings("DuplicatedCode")
    protected void addUserLoginLog(long jwtTokenId, HttpServletRequest request, LoginSuccessEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Date now = new Date();
        // 记录登录成功日志user_login_log
        AbstractUserLoginReq loginData = event.getLoginData();
        UserInfo userInfo = event.getUserInfo();
        Assert.notNull(loginData, "loginData不能为null");
        Assert.notNull(userInfo, "userInfo不能为null");
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
        loginLog.setLoginType(loginData.getLoginType().getId());
        loginLog.setLoginState(EnumConstant.USER_LOGIN_LOG_LOGIN_STATE_1);
        loginLog.setRequestData(JacksonMapper.getInstance().toJson(loginData));
        loginLog.setJwtTokenId(jwtTokenId);
        loginLog.setCreateAt(now);
        queryDSL.insert(sysLoginLog).populate(loginLog).execute();
        log.debug("### 登录成功 -> loginTime={} | loginIp={}", loginLog.getLoginTime(), loginLog.getLoginIp());
    }

    protected void clearLoginFailedCount(LoginSuccessEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        AbstractUserLoginReq loginData = event.getLoginData();
        UserInfo userInfo = event.getUserInfo();
        Assert.notNull(loginData, "loginData不能为null");
        Assert.notNull(userInfo, "userInfo不能为null");
        SysLoginFailedCount loginFailedCount = queryDSL.selectFrom(sysLoginFailedCount)
                .where(sysLoginFailedCount.deleteFlag.eq(EnumConstant.LOGIN_FAILED_COUNT_DELETE_FLAG_0))
                .where(sysLoginFailedCount.userId.eq(userInfo.getUserId()))
                .where(sysLoginFailedCount.loginType.eq(loginData.getLoginType().getId()))
                .fetchOne();
        if (loginFailedCount != null) {
            SecurityDataSource.getQueryDSL().update(sysLoginFailedCount)
                    .set(sysLoginFailedCount.deleteFlag, EnumConstant.LOGIN_FAILED_COUNT_DELETE_FLAG_1)
                    .set(sysLoginFailedCount.updateAt, new Date())
                    .where(sysLoginFailedCount.deleteFlag.eq(EnumConstant.LOGIN_FAILED_COUNT_DELETE_FLAG_0))
                    .where(sysLoginFailedCount.userId.eq(userInfo.getUserId()))
                    .where(sysLoginFailedCount.loginType.eq(loginData.getLoginType().getId()))
                    .execute();
            log.debug("### 清除用户连续登录失败次数: {} | userId=[{}]", loginFailedCount.getFailedCount(), loginFailedCount.getUserId());
        }
    }

    protected void disableFirstToken(LoginSuccessEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        final Date now = new Date();
        LoginConfig loginConfig = event.getLoginConfig();
        if (loginConfig.getConcurrentLoginCount() <= 0) {
            return;
        }
        AbstractUserLoginReq loginData = event.getLoginData();
        UserInfo userInfo = event.getUserInfo();
        Assert.notNull(loginData, "loginData不能为null");
        Assert.notNull(userInfo, "userInfo不能为null");
        if (loginConfig.isAllowAfterLogin()) {
            // 获取当前用户并发登录数量
            long realConcurrentLoginCount = queryDSL.selectFrom(sysJwtToken)
                    .where(sysJwtToken.userId.eq(userInfo.getUserId()))
                    .where(sysJwtToken.disable.eq(EnumConstant.DISABLE_0))
                    .where(sysJwtToken.expiredTime.isNotNull().and(sysJwtToken.expiredTime.gt(now)))
                    .fetchCount();
            long disableCount = realConcurrentLoginCount - loginConfig.getConcurrentLoginCount();
            if (disableCount >= 1) {
                // 挤下最早登录的用户
                List<SysJwtToken> jwtTokenList = queryDSL.selectFrom(sysJwtToken)
                        .where(sysJwtToken.userId.eq(userInfo.getUserId()))
                        .where(sysJwtToken.disable.eq(EnumConstant.DISABLE_0))
                        .where(sysJwtToken.expiredTime.isNotNull().and(sysJwtToken.expiredTime.gt(now)))
                        .orderBy(sysJwtToken.createAt.asc())
                        .limit(disableCount)
                        .fetch();
                if (jwtTokenList != null && !jwtTokenList.isEmpty()) {
                    if (dataSource.isEnableRedis()) {
                        List<String> kes = jwtTokenList.stream()
                                .map(token -> SecurityRedisKey.getTokenKey(
                                        dataSource.getRedisNamespace(),
                                        Conv.asString(userInfo.getUserId()),
                                        Conv.asString(token.getId())
                                )).collect(Collectors.toList());
                        redis.kDelete(kes);
                    }
                    disableCount = queryDSL.update(sysJwtToken)
                            .set(sysJwtToken.disable, EnumConstant.DISABLE_1)
                            .set(sysJwtToken.disableReason, EnumConstant.JWT_TOKEN_DISABLE_REASON_2)
                            .set(sysJwtToken.updateAt, now)
                            .where(sysJwtToken.id.in(jwtTokenList.stream().map(SysJwtToken::getId).collect(Collectors.toSet())))
                            .execute();
                    log.debug("### 挤下最早登录的用户 -> userId={} | disableCount={}", userInfo.getUserId(), disableCount);
                }
            }
        }
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }
}
