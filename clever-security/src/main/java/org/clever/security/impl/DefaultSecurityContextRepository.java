package org.clever.security.impl;

import com.querydsl.core.Tuple;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.mapper.JacksonMapper;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.QueryDSL;
import org.clever.data.redis.Redis;
import org.clever.security.SecurityContextRepository;
import org.clever.security.SecurityDataSource;
import org.clever.security.config.DataSourceConfig;
import org.clever.security.config.SecurityConfig;
import org.clever.security.exception.AuthenticationInnerException;
import org.clever.security.impl.model.EnumConstant;
import org.clever.security.impl.model.entity.SysSecurityContext;
import org.clever.security.impl.model.entity.SysUser;
import org.clever.security.impl.model.query.QSysSecurityContext;
import org.clever.security.impl.utils.UserInfoConvertUtils;
import org.clever.security.model.LoginContext;
import org.clever.security.model.SecurityContext;
import org.clever.security.utils.SecurityContextEqualsUtils;
import org.clever.security.utils.SecurityRedisKey;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.clever.security.impl.model.query.QSysResource.sysResource;
import static org.clever.security.impl.model.query.QSysRole.sysRole;
import static org.clever.security.impl.model.query.QSysRoleResource.sysRoleResource;
import static org.clever.security.impl.model.query.QSysSecurityContext.sysSecurityContext;
import static org.clever.security.impl.model.query.QSysUser.sysUser;
import static org.clever.security.impl.model.query.QSysUserRole.sysUserRole;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/14 21:29 <br/>
 */
@Slf4j
public class DefaultSecurityContextRepository implements SecurityContextRepository {
    @Override
    public void cacheContext(LoginContext context, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response) {
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        final Long userId = context.getUserInfo().getUserId();
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        SecurityContext securityContext = queryDSL.beginTX(status -> {
            // 锁住用户行(等待SecurityContext更新完毕)
            queryDSL.select(sysUser.id).from(sysUser).where(sysUser.id.eq(userId)).forUpdate().fetchOne();
            // 获取最新的 SecurityContext
            TupleTwo<SysSecurityContext, SecurityContext> tupleTwo = newSecurityContext(userId);
            if (tupleTwo == null) {
                queryDSL.delete(sysSecurityContext)
                    .where(sysSecurityContext.userId.eq(userId))
                    .execute();
                return null;
            }
            SysSecurityContext userSecurityContext = queryDSL.selectFrom(sysSecurityContext).where(sysSecurityContext.userId.eq(userId)).fetchOne();
            // 不存在直接新增
            if (userSecurityContext == null) {
                queryDSL.insert(sysSecurityContext).populate(tupleTwo.getValue1()).execute();
                return tupleTwo.getValue2();
            }
            SecurityContext sc = JacksonMapper.getInstance().fromJson(userSecurityContext.getSecurityContext(), SecurityContext.class);
            // 存在则比对刷新
            if (SecurityContextEqualsUtils.equals(sc, tupleTwo.getValue2())) {
                return tupleTwo.getValue2();
            }
            queryDSL.update(sysSecurityContext)
                .set(sysSecurityContext.securityContext, JacksonMapper.getInstance().toJson(tupleTwo.getValue2()))
                .set(sysSecurityContext.updateAt, new Date())
                .where(sysSecurityContext.id.eq(userSecurityContext.getId()))
                .execute();
            return tupleTwo.getValue2();
        });
        // SecurityContext写入Redis缓存
        if (dataSource.isEnableRedis() && securityContext != null) {
            redis.vSet(SecurityRedisKey.getSecurityContextKey(dataSource.getRedisNamespace(), Conv.asString(userId)), securityContext);
        }
        log.debug("### 缓存SecurityConfig成功 -> userId={}", userId);
    }

    @Override
    public SecurityContext loadContext(Long userId, Claims claims, SecurityConfig securityConfig, HttpServletRequest request, HttpServletResponse response) {
        final DataSourceConfig dataSource = securityConfig.getDataSource();
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        final Redis redis = SecurityDataSource.getRedis();
        SecurityContext securityContext = null;
        // 从Redis缓存中读SecurityContext
        if (dataSource.isEnableRedis()) {
            securityContext = redis.vGet(SecurityRedisKey.getSecurityContextKey(dataSource.getRedisNamespace(), Conv.asString(userId)), SecurityContext.class);
        }
        if (securityContext == null) {
            // 从数据库中读SecurityContext
            securityContext = queryDSL.beginTX(status -> {
                // 锁住用户行(等待SecurityContext更新完毕)
                queryDSL.select(sysUser.id).from(sysUser).where(sysUser.id.eq(Conv.asLong(userId))).forUpdate().fetchOne();
                // 开始加载SecurityContext
                SysSecurityContext userSecurityContext = queryDSL.selectFrom(sysSecurityContext)
                    .where(sysSecurityContext.userId.eq(Conv.asLong(userId)))
                    .fetchOne();
                if (userSecurityContext == null) {
                    TupleTwo<SysSecurityContext, SecurityContext> tupleTwo = newSecurityContext(userId);
                    if (tupleTwo == null) {
                        return null;
                    }
                    queryDSL.insert(sysSecurityContext).populate(tupleTwo.getValue1()).execute();
                    return tupleTwo.getValue2();
                }
                return JacksonMapper.getInstance().fromJson(userSecurityContext.getSecurityContext(), SecurityContext.class);
            });
            // SecurityContext写入Redis缓存
            if (dataSource.isEnableRedis() && securityContext != null) {
                redis.vSet(SecurityRedisKey.getSecurityContextKey(dataSource.getRedisNamespace(), Conv.asString(userId)), securityContext);
            }
        }
        if (securityContext == null) {
            throw new AuthenticationInnerException("加载SecurityConfig失败");
        }
        log.debug("### 加载SecurityConfig成功 -> userId={}", userId);
        return securityContext;
    }

    /**
     * 创建 SysSecurityContext 和 SecurityContext
     *
     * @param userId 用户id
     */
    protected TupleTwo<SysSecurityContext, SecurityContext> newSecurityContext(Long userId) {
        final QueryDSL queryDSL = SecurityDataSource.getQueryDSL();
        SysUser user = queryDSL.select(sysUser)
            .from(sysUser)
            .where(sysUser.id.eq(userId))
            .fetchOne();
        if (user == null) {
            return null;
        }
        // 查询角色
        List<Tuple> roles = queryDSL.select(sysRole.roleCode, sysRole.id)
            .from(sysUserRole).leftJoin(sysRole).on(sysUserRole.roleId.eq(sysRole.id))
            .where(sysRole.isEnable.eq(EnumConstant.ENABLED_1))
            .where(sysUserRole.userId.eq(userId))
            .fetch();
        // 查询权限
        List<String> rolePermissions = queryDSL.select(sysResource.permission).distinct()
            .from(sysResource).leftJoin(sysRoleResource).on(sysResource.id.eq(sysRoleResource.resourceId))
            .where(sysResource.permission.isNotNull())
            .where(sysResource.isEnable.eq(EnumConstant.ENABLED_1))
            .where(sysRoleResource.roleId.in(roles.stream().map(item -> item.get(sysRole.id)).collect(Collectors.toSet())))
            .fetch();
        // SecurityContext
        SecurityContext securityContext = new SecurityContext(
            UserInfoConvertUtils.convertToUserInfo(user),
            roles.stream().map(item -> item.get(sysRole.id)).map(String::valueOf).collect(Collectors.toSet()),
            new HashSet<>(rolePermissions)
        );
        // SysSecurityContext
        SysSecurityContext sysSecurityContext = new SysSecurityContext();
        // sysSecurityContext.setId(SnowFlake.SNOW_FLAKE.nextId());
        sysSecurityContext.setId(queryDSL.nextId(QSysSecurityContext.sysSecurityContext.getTableName()));
        sysSecurityContext.setUserId(userId);
        sysSecurityContext.setSecurityContext(JacksonMapper.getInstance().toJson(securityContext));
        sysSecurityContext.setCreateAt(new Date());
        return TupleTwo.creat(sysSecurityContext, securityContext);
    }
}
