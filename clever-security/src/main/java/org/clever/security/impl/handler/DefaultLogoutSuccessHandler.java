package org.clever.security.impl.handler;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.OrderIncrement;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.handler.LogoutSuccessHandler;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.model.UserInfo;
import org.clever.security.model.jackson2.event.LogoutSuccessEvent;
import org.clever.security.utils.SecurityRedisKey;

import java.util.Date;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/14 21:33 <br/>
 */
@Slf4j
public class DefaultLogoutSuccessHandler implements LogoutSuccessHandler {
    private final SecurityConfig securityConfig;

    public DefaultLogoutSuccessHandler(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, LogoutSuccessEvent event) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        // 禁用JWT-Token
        queryDSL.beginTX(status -> {
            disableJwtToken(event.getClaims());
            return null;
        });
        // 从Redis中删除SysJwtToken
        if (dataSource.isEnableRedis()) {
            UserInfo userInfo = event.getSecurityContext().getUserInfo();
            String key = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(userInfo.getUserId()), event.getClaims().getId());
            redis.kExpire(key, Redis.DEL_TIME_OUT);
        }
    }

    protected void disableJwtToken(Claims claims) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        queryDSL.update(sysJwtToken)
                .set(sysJwtToken.disable, EnumConstant.DISABLE_1)
                .set(sysJwtToken.disableReason, EnumConstant.JWT_TOKEN_DISABLE_REASON_3)
                .set(sysJwtToken.updateAt, new Date())
                .where(sysJwtToken.id.eq(Long.parseLong(claims.getId())))
                .execute();
        log.debug("### 登出成功 -> userId={} | token id={}", claims.getSubject(), claims.getId());
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }
}
