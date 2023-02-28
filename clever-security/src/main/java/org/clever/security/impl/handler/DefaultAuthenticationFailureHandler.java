package org.clever.security.impl.handler;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.OrderIncrement;
import org.clever.core.http.CookieUtils;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.handler.AuthenticationFailureHandler;
import org.clever.security.model.jackson2.event.AuthenticationFailureEvent;
import org.clever.security.utils.SecurityRedisKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/30 21:24 <br/>
 */
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final SecurityConfig securityConfig;

    public DefaultAuthenticationFailureHandler(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationFailureEvent event) {
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        final Redis redis = SecurityDataSource.getRedis();
        try {
            CookieUtils.delCookieForRooPath(request, response, securityConfig.getToken().getJwtTokenName());
        } catch (Exception ignored) {
        }
        if (dataSource.isEnableRedis()
                && event.getUserId() != null
                && event.getClaims() != null
                && StringUtils.isNotBlank(event.getClaims().getId())) {
            String key = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(event.getUserId()), event.getClaims().getId());
            redis.kExpire(key, Redis.DEL_TIME_OUT);
        }
    }

    @Override
    public double getOrder() {
        return OrderIncrement.MAX;
    }
}
