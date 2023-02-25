package org.clever.security.impl.authentication;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.Ordered;
import org.clever.core.SystemClock;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityDataSource;
import org.clever.security.authentication.token.VerifyJwtToken;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.AuthenticationException;
import org.clever.security.exception.InvalidJwtTokenException;
import org.clever.security.exception.LoginNameNotFoundException;
import org.clever.security.exception.UserDisabledException;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysJwtToken;
import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.utils.SecurityRedisKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Objects;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;
import static org.clever.security.impl.model.query.QSysUser.sysUser;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/14 21:22 <br/>
 */
@Slf4j
public class DefaultVerifyJwtToken implements VerifyJwtToken {
    @Override
    public void verify(String jwtToken, Long userId, Claims claims, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        // 校验JWT-Token
        verifyJwtToken(dataSource, userId, Conv.asLong(claims.getId()));
        // 判断账号是否可用
        verifyUser(dataSource, userId);
        log.debug("### JWT-Token验证成功 -> userId={} | Token={}", claims.getSubject(), jwtToken);
    }

    protected void verifyJwtToken(DataSourceConfig dataSource, Long userId, Long id) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        // 验证JWT-Token状态
        SysJwtToken jwtToken = null;
        // 从Redis缓存读取SysJwtToken
        if (dataSource.isEnableRedis()) {
            jwtToken = redis.vGet(SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(userId), Conv.asString(id)), SysJwtToken.class);
        }
        // 从数据库中读取SysJwtToken
        if (jwtToken == null) {
            jwtToken = queryDSL.selectFrom(sysJwtToken).where(sysJwtToken.id.eq(id)).fetchOne();
            // SysJwtToken缓存在Redis中
            if (dataSource.isEnableRedis() && jwtToken != null) {
                String key = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(userId), Conv.asString(jwtToken.getId()));
                redis.vSet(key, jwtToken, jwtToken.getExpiredTime().getTime() - SystemClock.now());
            }
        }
        if (jwtToken == null) {
            // 无效的 Token
            throw new InvalidJwtTokenException("无效的Token");
        }
        Date now = new Date();
        if (jwtToken.getExpiredTime() != null && now.compareTo(jwtToken.getExpiredTime()) >= 0) {
            // 已过期
            throw new InvalidJwtTokenException("Token已过期");
        }
        if (!Objects.equals(jwtToken.getDisable(), EnumConstant.DISABLE_0)) {
            // 已禁用
            String msg = "Token已禁用";
            if (jwtToken.getDisableReason() != null) {
                switch (jwtToken.getDisableReason()) {
                    case EnumConstant.JWT_TOKEN_DISABLE_REASON_0:
                        msg = "Token已失效(使用refresh token)";
                        break;
                    case EnumConstant.JWT_TOKEN_DISABLE_REASON_1:
                        msg = "Token已禁用";
                        break;
                    case EnumConstant.JWT_TOKEN_DISABLE_REASON_2:
                        msg = "Token已失效(被挤下线)";
                        break;
                    case EnumConstant.JWT_TOKEN_DISABLE_REASON_3:
                        msg = "Token已失效(已登出)";
                        break;
                }
            }
            throw new InvalidJwtTokenException(msg);
        }
    }

    protected void verifyUser(DataSourceConfig dataSource, Long userId) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        final long timeout = 1000 * 60 * 30;
        SysUser user = null;
        if (dataSource.isEnableRedis()) {
            user = redis.vGet(SecurityRedisKey.getUserKey(dataSource.getRedisNamespace(), Conv.asString(userId)), SysUser.class);
        }
        if (user == null) {
            user = queryDSL.selectFrom(sysUser).where(sysUser.id.eq(userId)).fetchOne();
            if (dataSource.isEnableRedis() && user != null) {
                redis.vSet(SecurityRedisKey.getUserKey(dataSource.getRedisNamespace(), Conv.asString(userId)), user, timeout);
            }
        }
        if (user == null) {
            throw new LoginNameNotFoundException("登录名不存在");
        }
        if (!Objects.equals(user.getIsEnable(), EnumConstant.ENABLED_1)) {
            throw new UserDisabledException("登录账号被禁用");
        }
    }

    @Override
    public double getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
