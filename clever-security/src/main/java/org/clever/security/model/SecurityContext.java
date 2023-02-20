package org.clever.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.clever.util.Assert;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 21:23 <br/>
 */
@Data
public class SecurityContext implements Principal, Serializable {
    /**
     * 用户信息
     */
    private final UserInfo userInfo;
    /**
     * 角色列表
     */
    private final Set<String> roles;
    /**
     * 权限列表
     */
    private final Set<String> permissions;
    /**
     * 扩展信息(与用户关联的业务数据，这个字段不会缓存)
     */
    @JsonIgnore
    private final ConcurrentMap<String, Object> ext = new ConcurrentHashMap<>();

    public SecurityContext(UserInfo userInfo, Set<String> roles, Set<String> permissions) {
        Assert.notNull(userInfo, "参数userInfo不能为null");
        this.userInfo = userInfo;
        if (roles == null) {
            this.roles = Collections.emptySet();
        } else {
            this.roles = Collections.unmodifiableSet(roles);
        }
        if (permissions == null) {
            this.permissions = Collections.emptySet();
        } else {
            this.permissions = Collections.unmodifiableSet(permissions);
        }
    }

    /**
     * 是否拥有指定全部角色
     */
    public boolean hasRoles(String... roles) {
        return hasAll(this.roles, roles);
    }

    /**
     * 是否拥有指定全部权限
     */
    public boolean hasPermissions(String... permissions) {
        return hasAll(this.permissions, permissions);
    }

    /**
     * 是否拥有指定任意角色
     */
    public boolean hasAnyRoles(String... roles) {
        return hasAny(this.roles, roles);
    }

    /**
     * 是否拥有指定任意权限
     */
    public boolean hasAnyPermissions(String... permissions) {
        return hasAny(this.permissions, permissions);
    }

    protected boolean hasAll(Set<String> source, String... target) {
        if (target == null || target.length == 0) {
            return true;
        }
        if (source == null || source.isEmpty()) {
            return false;
        }
        boolean flag = true;
        for (String str : target) {
            if (!source.contains(str)) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    protected boolean hasAny(Set<String> source, String... target) {
        if (target == null || target.length == 0) {
            return true;
        }
        if (source == null || source.isEmpty()) {
            return false;
        }
        boolean flag = false;
        for (String str : target) {
            if (source.contains(str)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public SecurityContext copy() {
        UserInfo userInfo = this.userInfo.copy();
        return new SecurityContext(userInfo, roles, permissions);
    }

    @Override
    public String getName() {
        return userInfo.getUserName();
    }
}
