package org.clever.security.impl.authentication;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.DateUtils;
import org.clever.core.SystemClock;
import org.clever.core.mapper.BeanMapper;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityDataSource;
import org.clever.security.authentication.token.RefreshJwtToken;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysJwtToken;
import org.clever.security.model.NewJwtToken;
import org.clever.security.model.UseJwtRefreshToken;
import org.clever.security.utils.SecurityRedisKey;

import java.util.Date;
import java.util.Objects;

import static org.clever.security.impl.model.query.QSysJwtToken.sysJwtToken;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 13:31 <br/>
 */
public class DefaultRefreshJwtToken implements RefreshJwtToken {
    private final SecurityConfig securityConfig;

    public DefaultRefreshJwtToken(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public NewJwtToken refresh(UseJwtRefreshToken useJwtRefreshToken) {
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        final Date now = new Date();
        // 开启事务
        return SecurityDataSource.getJdbc().beginTX(status -> {
            SysJwtToken useToken = queryDSL.selectFrom(sysJwtToken).where(sysJwtToken.id.eq(useJwtRefreshToken.getUseJwtId())).forUpdate().fetchOne();
            // 当token过期时，前端可能同时发送多个请求(并发)，而此时这些请求都带有相同的token和refreshToken，所以服务端对于这些请求需要返回相同的NewJwtToken
            if (useToken != null
                    && useToken.getRtCreateTokenId() != null
                    && useToken.getRtUseTime() != null
                    && DateUtils.pastSeconds(useToken.getRtUseTime(), now) <= 60) {
                SysJwtToken rtCreateToken = queryDSL.select(sysJwtToken).from(sysJwtToken).where(sysJwtToken.id.eq(useToken.getRtCreateTokenId())).fetchOne();
                if (rtCreateToken != null) {
                    return BeanMapper.mapper(rtCreateToken, NewJwtToken.class);
                }
            }
            // 验证当前Token，异常场景 --> 1.token不存在，2.token未过期，3.token被禁用，4.刷新Token值为空，5.刷新Token内容不一致，6.刷新Token已过期，7.刷新Token无效
            if (useToken == null
                    || (useToken.getExpiredTime() != null && now.compareTo(useToken.getExpiredTime()) < 0)
                    || !Objects.equals(useToken.getDisable(), EnumConstant.DISABLE_0)
                    || StringUtils.isBlank(useToken.getRefreshToken())
                    || !Objects.equals(useJwtRefreshToken.getUseRefreshToken(), useToken.getRefreshToken())
                    || (useToken.getRtExpiredTime() != null && now.compareTo(useToken.getRtExpiredTime()) >= 0)
                    || !Objects.equals(useToken.getRtState(), EnumConstant.JWT_TOKEN_REFRESH_TOKEN_STATE_1)) {
                return null;
            }
            // 新增Token
            SysJwtToken add = new SysJwtToken();
            add.setId(useJwtRefreshToken.getJwtId());
            add.setUserId(useToken.getUserId());
            add.setToken(useJwtRefreshToken.getToken());
            add.setExpiredTime(DateUtils.toTimestamp(useJwtRefreshToken.getExpiredTime()));
            add.setDisable(EnumConstant.DISABLE_0);
            add.setRefreshToken(useJwtRefreshToken.getRefreshToken());
            add.setRtExpiredTime(DateUtils.toTimestamp(useJwtRefreshToken.getRefreshTokenExpiredTime()));
            add.setRtState(EnumConstant.JWT_TOKEN_REFRESH_TOKEN_STATE_1);
            add.setCreateAt(DateUtils.toTimestamp(now));
            queryDSL.insert(sysJwtToken).populate(add).execute();
            // 更新Token
            SysJwtToken update = new SysJwtToken();
            update.setDisable(EnumConstant.DISABLE_1);
            update.setDisableReason(EnumConstant.JWT_TOKEN_DISABLE_REASON_0);
            update.setRtState(EnumConstant.JWT_TOKEN_REFRESH_TOKEN_STATE_0);
            update.setRtUseTime(DateUtils.toTimestamp(now));
            update.setRtCreateTokenId(add.getId());
            update.setUpdateAt(DateUtils.toTimestamp(now));
            queryDSL.update(sysJwtToken).populate(update).where(sysJwtToken.id.eq(useToken.getId())).execute();
            // 更新Redis缓存
            if (dataSource.isEnableRedis()) {
                String delKey = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(useToken.getUserId()), Conv.asString(update.getId()));
                String addKey = SecurityRedisKey.getTokenKey(dataSource.getRedisNamespace(), Conv.asString(useToken.getUserId()), Conv.asString(add.getId()));
                redis.kDelete(delKey);
                redis.vSet(addKey, add, add.getExpiredTime().getTime() - SystemClock.now());
            }
            // 返回新增的Token
            return BeanMapper.mapper(add, NewJwtToken.class);
        });
    }
}
