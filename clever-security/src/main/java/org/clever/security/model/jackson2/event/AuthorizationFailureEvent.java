package org.clever.security.model.jackson2.event;

import lombok.Data;
import org.clever.security.model.UserInfo;

import java.io.Serializable;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/06 21:15 <br/>
 */
@Data
public class AuthorizationFailureEvent implements Serializable {
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

    public AuthorizationFailureEvent(UserInfo userInfo, Set<String> roles, Set<String> permissions) {
        this.userInfo = userInfo;
        this.roles = roles;
        this.permissions = permissions;
    }
}
